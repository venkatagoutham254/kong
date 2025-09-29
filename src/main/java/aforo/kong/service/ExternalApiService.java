package aforo.kong.service;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.dto.KongProductResponse;
import aforo.kong.entity.KongProduct;

import java.util.List;

public interface ExternalApiService {
    KongProductResponse fetchProducts(ClientApiDetailsDTO apiDetails);
    List<KongProduct> getAllProducts();
    KongProduct getProductById(String id);
    void deleteProductById(String id);
    List<KongProductResponse> fetchProductsFromDb(Long clientDetailsId);

}
