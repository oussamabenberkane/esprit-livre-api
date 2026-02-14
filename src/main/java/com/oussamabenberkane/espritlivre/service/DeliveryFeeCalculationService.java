package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.domain.enumeration.DeliveryFeeMethod;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import com.oussamabenberkane.espritlivre.repository.BookPackRepository;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.service.dto.DeliveryFeeCalculationRequest;
import com.oussamabenberkane.espritlivre.service.dto.DeliveryFeeCalculationResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.DeliveryFeeResult;
import com.oussamabenberkane.espritlivre.service.shipping.YalidineService;
import com.oussamabenberkane.espritlivre.service.shipping.ZrExpressService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for calculating delivery fees for checkout.
 * Handles both fixed fees (from product settings) and automatic calculation via shipping provider APIs.
 */
@Service
public class DeliveryFeeCalculationService {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryFeeCalculationService.class);

    private final BookRepository bookRepository;
    private final BookPackRepository bookPackRepository;
    private final YalidineService yalidineService;
    private final ZrExpressService zrExpressService;

    public DeliveryFeeCalculationService(
        BookRepository bookRepository,
        BookPackRepository bookPackRepository,
        YalidineService yalidineService,
        ZrExpressService zrExpressService
    ) {
        this.bookRepository = bookRepository;
        this.bookPackRepository = bookPackRepository;
        this.yalidineService = yalidineService;
        this.zrExpressService = zrExpressService;
    }

    /**
     * Calculate delivery fee for a cart.
     * For each item:
     * - If automaticDeliveryFee is enabled: calculate via shipping provider API
     * - Otherwise: use the fixed deliveryFee from the product
     *
     * The final fee is the MINIMUM of all calculated fees.
     *
     * @param request The calculation request with cart items and destination
     * @return The calculation response with fee and audit info
     */
    public DeliveryFeeCalculationResponse calculateDeliveryFee(DeliveryFeeCalculationRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return DeliveryFeeCalculationResponse.failure("No items provided");
        }

        if (request.getWilaya() == null || request.getWilaya().isBlank()) {
            return DeliveryFeeCalculationResponse.failure("Wilaya is required");
        }

        if (request.getShippingProvider() == null) {
            return DeliveryFeeCalculationResponse.failure("Shipping provider is required");
        }

        List<ItemFeeCalculation> itemCalculations = new ArrayList<>();
        boolean hasAutomaticCalculation = false;
        boolean allSuccessful = true;
        String firstError = null;

        // Calculate fee for each item
        for (DeliveryFeeCalculationRequest.CartItem item : request.getItems()) {
            ItemFeeCalculation calc = calculateItemFee(item, request);

            if (!calc.success) {
                allSuccessful = false;
                if (firstError == null) {
                    firstError = calc.errorMessage;
                }
            } else {
                itemCalculations.add(calc);
                if (calc.method == DeliveryFeeMethod.AUTOMATIC) {
                    hasAutomaticCalculation = true;
                }
            }
        }

        if (itemCalculations.isEmpty()) {
            return DeliveryFeeCalculationResponse.failure(firstError != null ? firstError : "No fees could be calculated");
        }

        // Find the minimum fee among all items
        BigDecimal minFee = null;
        DeliveryFeeMethod resultMethod = DeliveryFeeMethod.FIXED;
        ShippingProvider resultProvider = null;

        for (ItemFeeCalculation calc : itemCalculations) {
            if (minFee == null || calc.fee.compareTo(minFee) < 0) {
                minFee = calc.fee;
                resultMethod = calc.method;
                resultProvider = calc.provider;
            }
        }

        // If any calculation used automatic, the overall method is automatic
        if (hasAutomaticCalculation && resultMethod == DeliveryFeeMethod.FIXED) {
            // The minimum was from a fixed fee, but we had automatic calculations
            // Keep the fixed method since that's what determined the final fee
        }

        LOG.info("Delivery fee calculated: {} DA, method={}, provider={} (from {} items)",
            minFee, resultMethod, resultProvider, itemCalculations.size());

        return DeliveryFeeCalculationResponse.success(minFee, resultMethod, resultProvider);
    }

    /**
     * Calculate delivery fee for a single item.
     */
    private ItemFeeCalculation calculateItemFee(DeliveryFeeCalculationRequest.CartItem item,
                                                 DeliveryFeeCalculationRequest request) {
        // Determine product settings
        BigDecimal fixedFee = null;
        boolean useAutomatic = false;
        String productName = "Unknown";

        if (item.getBookId() != null) {
            Optional<Book> bookOpt = bookRepository.findById(item.getBookId());
            if (bookOpt.isEmpty()) {
                return ItemFeeCalculation.failure("Book not found: " + item.getBookId());
            }
            Book book = bookOpt.orElseThrow();
            productName = book.getTitle();
            fixedFee = book.getDeliveryFee();
            useAutomatic = Boolean.TRUE.equals(book.getAutomaticDeliveryFee());
        } else if (item.getBookPackId() != null) {
            Optional<BookPack> packOpt = bookPackRepository.findById(item.getBookPackId());
            if (packOpt.isEmpty()) {
                return ItemFeeCalculation.failure("BookPack not found: " + item.getBookPackId());
            }
            BookPack pack = packOpt.orElseThrow();
            productName = pack.getTitle();
            fixedFee = pack.getDeliveryFee();
            useAutomatic = Boolean.TRUE.equals(pack.getAutomaticDeliveryFee());
        } else {
            return ItemFeeCalculation.failure("Either bookId or bookPackId is required");
        }

        // Calculate the fee
        if (useAutomatic) {
            // Use shipping provider API to calculate
            DeliveryFeeResult result = calculateAutomaticFee(request);
            if (result.isSuccess()) {
                LOG.debug("Automatic fee for '{}': {} DA", productName, result.getFee());
                return ItemFeeCalculation.success(result.getFee(), DeliveryFeeMethod.AUTOMATIC, result.getProvider());
            } else {
                // Automatic calculation failed
                LOG.warn("Automatic fee calculation failed for '{}': {}", productName, result.getErrorMessage());
                return ItemFeeCalculation.failure(result.getErrorMessage());
            }
        } else {
            // Use fixed fee from product
            BigDecimal fee = fixedFee != null ? fixedFee : BigDecimal.ZERO;
            LOG.debug("Fixed fee for '{}': {} DA", productName, fee);
            return ItemFeeCalculation.success(fee, DeliveryFeeMethod.FIXED, null);
        }
    }

    /**
     * Calculate automatic delivery fee using shipping provider API.
     */
    private DeliveryFeeResult calculateAutomaticFee(DeliveryFeeCalculationRequest request) {
        boolean isStopDesk = Boolean.TRUE.equals(request.getIsStopDesk());

        if (request.getShippingProvider() == ShippingProvider.YALIDINE) {
            // Yalidine needs wilaya ID
            Integer wilayaId = getWilayaIdFromName(request.getWilaya());
            return yalidineService.getDeliveryFee(wilayaId, request.getCity(), isStopDesk);
        } else if (request.getShippingProvider() == ShippingProvider.ZR) {
            return zrExpressService.getDeliveryFee(request.getWilaya(), request.getCity(), isStopDesk);
        } else {
            return DeliveryFeeResult.failure("Unknown shipping provider: " + request.getShippingProvider());
        }
    }

    /**
     * Get wilaya ID from wilaya name for Yalidine API.
     */
    private Integer getWilayaIdFromName(String wilayaName) {
        if (wilayaName == null) {
            return null;
        }

        // Try to parse as number first (frontend might send ID directly)
        try {
            return Integer.parseInt(wilayaName.trim());
        } catch (NumberFormatException ignored) {
            // Not a number, try to map from name
        }

        String normalized = wilayaName.toLowerCase().trim();
        return switch (normalized) {
            case "adrar" -> 1;
            case "chlef" -> 2;
            case "laghouat" -> 3;
            case "oum el bouaghi" -> 4;
            case "batna" -> 5;
            case "bejaia", "béjaïa" -> 6;
            case "biskra" -> 7;
            case "bechar", "béchar" -> 8;
            case "blida" -> 9;
            case "bouira" -> 10;
            case "tamanrasset" -> 11;
            case "tebessa", "tébessa" -> 12;
            case "tlemcen" -> 13;
            case "tiaret" -> 14;
            case "tizi ouzou" -> 15;
            case "alger", "algiers" -> 16;
            case "djelfa" -> 17;
            case "jijel" -> 18;
            case "setif", "sétif" -> 19;
            case "saida", "saïda" -> 20;
            case "skikda" -> 21;
            case "sidi bel abbes", "sidi bel abbès" -> 22;
            case "annaba" -> 23;
            case "guelma" -> 24;
            case "constantine" -> 25;
            case "medea", "médéa" -> 26;
            case "mostaganem" -> 27;
            case "m'sila", "msila" -> 28;
            case "mascara" -> 29;
            case "ouargla" -> 30;
            case "oran" -> 31;
            case "el bayadh" -> 32;
            case "illizi" -> 33;
            case "bordj bou arreridj" -> 34;
            case "boumerdes", "boumerdès" -> 35;
            case "el tarf" -> 36;
            case "tindouf" -> 37;
            case "tissemsilt" -> 38;
            case "el oued" -> 39;
            case "khenchela" -> 40;
            case "souk ahras" -> 41;
            case "tipaza" -> 42;
            case "mila" -> 43;
            case "ain defla", "aïn defla" -> 44;
            case "naama", "naâma" -> 45;
            case "ain temouchent", "aïn témouchent" -> 46;
            case "ghardaia", "ghardaïa" -> 47;
            case "relizane" -> 48;
            case "timimoun" -> 49;
            case "bordj badji mokhtar" -> 50;
            case "ouled djellal" -> 51;
            case "beni abbes", "béni abbès" -> 52;
            case "in salah" -> 53;
            case "in guezzam" -> 54;
            case "touggourt" -> 55;
            case "djanet" -> 56;
            case "el meghaier", "el m'ghair" -> 57;
            case "el meniaa" -> 58;
            default -> null;
        };
    }

    /**
     * Internal class to hold item fee calculation result.
     */
    private static class ItemFeeCalculation {
        final boolean success;
        final BigDecimal fee;
        final DeliveryFeeMethod method;
        final ShippingProvider provider;
        final String errorMessage;

        private ItemFeeCalculation(boolean success, BigDecimal fee, DeliveryFeeMethod method,
                                   ShippingProvider provider, String errorMessage) {
            this.success = success;
            this.fee = fee;
            this.method = method;
            this.provider = provider;
            this.errorMessage = errorMessage;
        }

        static ItemFeeCalculation success(BigDecimal fee, DeliveryFeeMethod method, ShippingProvider provider) {
            return new ItemFeeCalculation(true, fee, method, provider, null);
        }

        static ItemFeeCalculation failure(String errorMessage) {
            return new ItemFeeCalculation(false, null, null, null, errorMessage);
        }
    }
}
