package aforo.kong.service;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.dto.KongProductResponse;
import aforo.kong.entity.KongProduct;

import java.util.List;
import java.util.Map;

public interface ExternalApiService {
    KongProductResponse fetchProducts(ClientApiDetailsDTO apiDetails);
    List<KongProduct> getAllProducts();
    KongProduct getProductById(String id);
    void deleteProductById(String id);
    List<KongProductResponse> fetchProductsFromDb(Long clientDetailsId);
    Map<String, Object> importSelectedProducts(List<String> productIds);
}
