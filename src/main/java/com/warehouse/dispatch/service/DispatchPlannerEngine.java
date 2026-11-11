package com.warehouse.dispatch.service;

import com.warehouse.dispatch.dto.*;
import com.warehouse.dispatch.util.DispatchDateUtil;
import com.warehouse.dispatch.util.PricingCalculatorUtil;
import com.warehouse.dispatch.util.WeightCalculatorUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DispatchPlannerEngine {

    private static final BigDecimal TRUCK_WEIGHT_CAPACITY = new BigDecimal("500.0");
    private static final int TRUCK_PACKAGE_CAPACITY = 40;

    /**
     * Executes transformations T1 through T10 deterministically in-memory.
     */
    public PlanResult plan(BatchDispatchRequest request, ReferenceData refData) {
        // T5: Effective dispatch date
        LocalDate effectiveDispatchDate = DispatchDateUtil.calculateEffectiveDispatchDate(
                request.requestedAt(),
                request.warehouse().cutoffTime(),
                request.dispatchDate(),
                refData.holidays()
        );

        // Pre-scan for DUPLICATE_ORDER within the incoming batch (both get rejected)
        Map<String, Long> batchOrderCounts = request.orders().stream()
                .collect(Collectors.groupingBy(OrderDto::orderNo, Collectors.counting()));

        List<RejectedOrderDto> rejectedOrders = new ArrayList<>();
        List<ValidOrderIntermediate> validOrders = new ArrayList<>();

        for (OrderDto order : request.orders()) {
            String rejectionReason = validateOrder(order, batchOrderCounts, effectiveDispatchDate, refData);
            if (rejectionReason != null) {
                String message = getRejectionMessage(rejectionReason, order, refData);
                rejectedOrders.add(new RejectedOrderDto(order.orderNo(), rejectionReason, message));
            } else {
                BigDecimal chargeableKg = WeightCalculatorUtil.calculateOrderChargeableKg(order.packages());
                boolean isHazmat = order.packages().stream().anyMatch(PackageDto::hazmat);
                String prefix = order.deliveryAddress().pincode().substring(0, 3);
                String zone = refData.activePincodeToZone().get(prefix);

                validOrders.add(new ValidOrderIntermediate(order, zone, isHazmat, chargeableKg));
            }
        }

        // If all orders are rejected -> FAILED status
        if (validOrders.isEmpty()) {
            SummaryDto summary = new SummaryDto(request.orders().size(), 0, rejectedOrders.size(), 0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return new PlanResult(request.batchId(), "FAILED", effectiveDispatchDate, summary, List.of(), rejectedOrders);
        }

        // T6: Grouping (zone ASC -> EXPRESS before STANDARD -> non-hazmat before hazmat)
        Comparator<GroupKey> groupComparator = Comparator
                .comparing(GroupKey::zone)
                .thenComparing((g1, g2) -> {
                    if (g1.serviceLevel().equalsIgnoreCase(g2.serviceLevel())) return 0;
                    return "EXPRESS".equalsIgnoreCase(g1.serviceLevel()) ? -1 : 1;
                })
                .thenComparing(GroupKey::hazmat); // false (non-hazmat) before true (hazmat)

        Map<GroupKey, List<ValidOrderIntermediate>> groupedOrders = new TreeMap<>(groupComparator);
        for (ValidOrderIntermediate vo : validOrders) {
            GroupKey key = new GroupKey(vo.zone(), vo.order().serviceLevel(), vo.isHazmat());
            groupedOrders.computeIfAbsent(key, k -> new ArrayList<>()).add(vo);
        }

        // T7 & T8 & T9: Load Building (First-Fit Decreasing), Carrier Selection, and Allocation
        List<PlanResult.PlannedLoad> plannedLoads = new ArrayList<>();
        int loadSequenceCounter = 1;

        for (Map.Entry<GroupKey, List<ValidOrderIntermediate>> entry : groupedOrders.entrySet()) {
            GroupKey key = entry.getKey();
            List<ValidOrderIntermediate> ordersInGroup = entry.getValue();

            // First-Fit Decreasing: sort orders by chargeableKg DESC, tie: orderNo ASC
            ordersInGroup.sort(Comparator
                    .comparing(ValidOrderIntermediate::chargeableKg).reversed()
                    .thenComparing(vo -> vo.order().orderNo()));

            List<OpenLoad> loadsInGroup = new ArrayList<>();

            for (ValidOrderIntermediate order : ordersInGroup) {
                int pkgCount = order.order().packages().size();
                OpenLoad targetLoad = null;

                for (OpenLoad load : loadsInGroup) {
                    if (load.canFit(order.chargeableKg(), pkgCount)) {
                        targetLoad = load;
                        break;
                    }
                }

                if (targetLoad == null) {
                    targetLoad = new OpenLoad();
                    loadsInGroup.add(targetLoad);
                }
                targetLoad.addOrder(order);
            }

            // For each formed load: pick carrier (T8), allocate freight (T9), calculate ETA (T10)
            for (OpenLoad openLoad : loadsInGroup) {
                String loadId = String.format("LD-%s-%02d", request.batchId(), loadSequenceCounter++);
                CarrierQuote bestCarrier = pickBestCarrier(key, openLoad.totalChargeableKg, effectiveDispatchDate, refData);

                LocalDate etaDate = DispatchDateUtil.calculateEta(effectiveDispatchDate, bestCarrier.transitDays(), refData.holidays());

                // T9: Cost Allocation
                List<PlanResult.PlannedOrderAllocation> allocatedOrders = allocateLoadFreight(
                        openLoad.orders, bestCarrier.loadCost(), openLoad.totalChargeableKg, refData, request.dispatchDate()
                );

                plannedLoads.add(new PlanResult.PlannedLoad(
                        loadId,
                        key.zone(),
                        key.serviceLevel(),
                        key.hazmat(),
                        bestCarrier.carrierCode(),
                        openLoad.totalPackageCount,
                        openLoad.totalChargeableKg.setScale(1, RoundingMode.HALF_UP),
                        bestCarrier.baseCharge(),
                        bestCarrier.weightCharge(),
                        bestCarrier.fuelSurcharge(),
                        bestCarrier.loadCost(),
                        etaDate,
                        allocatedOrders
                ));
            }
        }

        BigDecimal totalFreight = plannedLoads.stream()
                .map(PlanResult.PlannedLoad::loadCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String status = rejectedOrders.isEmpty() ? "COMPLETED" : "PARTIAL";
        SummaryDto summary = new SummaryDto(
                request.orders().size(),
                validOrders.size(),
                rejectedOrders.size(),
                plannedLoads.size(),
                totalFreight
        );

        return new PlanResult(request.batchId(), status, effectiveDispatchDate, summary, plannedLoads, rejectedOrders);
    }

    private String validateOrder(OrderDto order, Map<String, Long> batchOrderCounts, LocalDate effectiveDispatchDate, ReferenceData refData) {
        // 1. DUPLICATE_ORDER
        if (batchOrderCounts.get(order.orderNo()) > 1 || refData.existingOrderNumbers().contains(order.orderNo())) {
            return "DUPLICATE_ORDER";
        }

        // 2. UNSERVICEABLE
        String prefix = order.deliveryAddress().pincode().substring(0, 3);
        String zone = refData.activePincodeToZone().get(prefix);
        if (zone == null) {
            return "UNSERVICEABLE";
        }

        // 3. HAZMAT_EXPRESS_NOT_ALLOWED
        boolean hasHazmat = order.packages().stream().anyMatch(PackageDto::hazmat);
        if (hasHazmat && "EXPRESS".equalsIgnoreCase(order.serviceLevel())) {
            return "HAZMAT_EXPRESS_NOT_ALLOWED";
        }

        // 4. OVERWEIGHT
        BigDecimal totalChargeableKg = WeightCalculatorUtil.calculateOrderChargeableKg(order.packages());
        if (totalChargeableKg.compareTo(TRUCK_WEIGHT_CAPACITY) > 0) {
            return "OVERWEIGHT";
        }

        // 5. FX_RATE_MISSING
        String currency = order.declaredValue().currency().toUpperCase();
        if (!"INR".equals(currency) && (refData.fxRatesForDate() == null || !refData.fxRatesForDate().containsKey(currency))) {
            return "FX_RATE_MISSING";
        }

        // 6. NO_CARRIER
        boolean carrierExists = refData.carrierRates().stream()
                .filter(c -> c.isEffectiveOn(effectiveDispatchDate))
                .filter(c -> c.zoneCode().equalsIgnoreCase(zone))
                .filter(c -> c.serviceLevel().equalsIgnoreCase(order.serviceLevel()))
                .anyMatch(c -> !hasHazmat || c.hazmatOk());

        if (!carrierExists) {
            return "NO_CARRIER";
        }

        return null;
    }

    private String getRejectionMessage(String errorCode, OrderDto order, ReferenceData refData) {
        return switch (errorCode) {
            case "UNSERVICEABLE" -> "No zone for pincode prefix " + order.deliveryAddress().pincode().substring(0, 3);
            case "DUPLICATE_ORDER" -> "Order number already exists or duplicated in batch: " + order.orderNo();
            case "HAZMAT_EXPRESS_NOT_ALLOWED" -> "Hazmat packages not allowed on EXPRESS service level";
            case "OVERWEIGHT" -> "Order chargeable weight exceeds truck capacity of 500.0 kg";
            case "FX_RATE_MISSING" -> "Missing FX rate for currency " + order.declaredValue().currency();
            case "NO_CARRIER" -> "No eligible carrier for order requirements on effective dispatch date";
            default -> "Order validation failed";
        };
    }

    private CarrierQuote pickBestCarrier(GroupKey key, BigDecimal loadKg, LocalDate effectiveDate, ReferenceData refData) {
        List<ReferenceData.CarrierRateRecord> eligible = refData.carrierRates().stream()
                .filter(c -> c.isEffectiveOn(effectiveDate))
                .filter(c -> c.zoneCode().equalsIgnoreCase(key.zone()))
                .filter(c -> c.serviceLevel().equalsIgnoreCase(key.serviceLevel()))
                .filter(c -> !key.hazmat() || c.hazmatOk())
                .toList();

        List<CarrierQuote> quotes = new ArrayList<>();
        for (ReferenceData.CarrierRateRecord rate : eligible) {
            BigDecimal weightCharge = rate.ratePerKg().multiply(loadKg).setScale(2, RoundingMode.HALF_UP);
            BigDecimal basePlusWeight = rate.baseCharge().add(weightCharge);
            BigDecimal fuelSurcharge = basePlusWeight.multiply(rate.fuelPct())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal loadCost = basePlusWeight.add(fuelSurcharge).setScale(2, RoundingMode.HALF_UP);

            quotes.add(new CarrierQuote(rate.carrierCode(), rate.baseCharge(), weightCharge, fuelSurcharge, loadCost, rate.reliabilityScore(), rate.transitDays()));
        }

        // Lowest loadCost -> tie: higher reliability -> tie: carrierCode ASC
        quotes.sort(Comparator
                .comparing(CarrierQuote::loadCost)
                .thenComparing(Comparator.comparingInt(CarrierQuote::reliabilityScore).reversed())
                .thenComparing(CarrierQuote::carrierCode));

        return quotes.get(0);
    }

    private List<PlanResult.PlannedOrderAllocation> allocateLoadFreight(
            List<ValidOrderIntermediate> orders, BigDecimal loadCost, BigDecimal loadKg, ReferenceData refData, LocalDate dispatchDate) {

        List<PlanResult.PlannedOrderAllocation> result = new ArrayList<>();
        BigDecimal allocatedSum = BigDecimal.ZERO;
        ValidOrderIntermediate heaviestOrder = orders.get(0);

        // Find heaviest order (tie: smallest orderNo)
        for (ValidOrderIntermediate o : orders) {
            if (o.chargeableKg().compareTo(heaviestOrder.chargeableKg()) > 0) {
                heaviestOrder = o;
            } else if (o.chargeableKg().compareTo(heaviestOrder.chargeableKg()) == 0
                    && o.order().orderNo().compareTo(heaviestOrder.order().orderNo()) < 0) {
                heaviestOrder = o;
            }
        }

        Map<String, BigDecimal> freightMap = new HashMap<>();
        for (ValidOrderIntermediate o : orders) {
            BigDecimal fraction = o.chargeableKg().divide(loadKg, 8, RoundingMode.HALF_UP);
            BigDecimal allocated = loadCost.multiply(fraction).setScale(2, RoundingMode.HALF_UP);
            freightMap.put(o.order().orderNo(), allocated);
            allocatedSum = allocatedSum.add(allocated);
        }

        // Remainder adjustment to heaviest order
        BigDecimal remainder = loadCost.subtract(allocatedSum);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal adjusted = freightMap.get(heaviestOrder.order().orderNo()).add(remainder);
            freightMap.put(heaviestOrder.order().orderNo(), adjusted);
        }

        for (ValidOrderIntermediate o : orders) {
            BigDecimal allocatedFreight = freightMap.get(o.order().orderNo());
            BigDecimal discount = PricingCalculatorUtil.calculateDiscount(allocatedFreight, o.order().customer().tier());
            BigDecimal declaredInr = PricingCalculatorUtil.convertToInr(
                    o.order().declaredValue().amount(), o.order().declaredValue().currency(), refData.fxRatesForDate()
            );
            BigDecimal insurance = PricingCalculatorUtil.calculateInsuranceCharge(declaredInr);
            BigDecimal orderCharge = PricingCalculatorUtil.calculateOrderCharge(allocatedFreight, discount, insurance);

            DeliveryAddressDto addr = o.order().deliveryAddress();
            String formattedAddr = String.format("%s, %s, %s - %s", addr.line1(), addr.city(), addr.state(), addr.pincode()).trim();

            result.add(new PlanResult.PlannedOrderAllocation(
                    o.order(), o.chargeableKg(), allocatedFreight, discount, declaredInr, insurance, orderCharge, formattedAddr
            ));
        }

        return result;
    }

    private record GroupKey(String zone, String serviceLevel, boolean hazmat) {}

    private record ValidOrderIntermediate(OrderDto order, String zone, boolean isHazmat, BigDecimal chargeableKg) {}

    private record CarrierQuote(String carrierCode, BigDecimal baseCharge, BigDecimal weightCharge,
                                BigDecimal fuelSurcharge, BigDecimal loadCost, int reliabilityScore, int transitDays) {}

    private static class OpenLoad {
        BigDecimal totalChargeableKg = BigDecimal.ZERO;
        int totalPackageCount = 0;
        final List<ValidOrderIntermediate> orders = new ArrayList<>();

        boolean canFit(BigDecimal orderKg, int pkgCount) {
            return totalChargeableKg.add(orderKg).compareTo(TRUCK_WEIGHT_CAPACITY) <= 0
                    && (totalPackageCount + pkgCount) <= TRUCK_PACKAGE_CAPACITY;
        }

        void addOrder(ValidOrderIntermediate order) {
            orders.add(order);
            totalChargeableKg = totalChargeableKg.add(order.chargeableKg());
            totalPackageCount += order.order().packages().size();
        }
    }
}
