package aforo.kong.service.impl;

import aforo.kong.dto.ClientApiDetailsDTO;
import aforo.kong.dto.KongProductDTO;
import aforo.kong.dto.KongProductResponse;
import aforo.kong.entity.ClientApiDetails;
import aforo.kong.entity.KongProduct;
import aforo.kong.mapper.KongProductMapper;
import aforo.kong.repository.KongProductRepository;
import aforo.kong.service.ExternalApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import aforo.kong.repository.ClientApiDetailsRepository;


import java.util.List;

@Service
public class ExternalApiServiceImpl implements ExternalApiService {

    private final RestTemplate restTemplate;
    private final KongProductRepository productRepository;
    private final KongProductMapper productMapper;
    private final ObjectMapper objectMapper;
    private final ClientApiDetailsRepository clientApiDetailsRepository;
    public ExternalApiServiceImpl(RestTemplate restTemplate,
                                  KongProductRepository productRepository,
                                  KongProductMapper productMapper,ClientApiDetailsRepository clientApiDetailsRepository,
                                  ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.clientApiDetailsRepository = clientApiDetailsRepository;
        this.objectMapper = objectMapper;
    }

    @Override
public KongProductResponse fetchProducts(ClientApiDetailsDTO apiDetails) {
    String url = buildUrl(apiDetails.getBaseUrl(), apiDetails.getEndpoint());

    HttpHeaders headers = new HttpHeaders();
    if (apiDetails.getAuthToken() != null && !apiDetails.getAuthToken().isBlank()) {
        // ✅ Use Bearer auth (this was the bug)
        headers.setBearerAuth(apiDetails.getAuthToken());
    }
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        KongProductResponse productResponse =
                objectMapper.readValue(response.getBody(), KongProductResponse.class);

        if (productResponse != null && productResponse.getData() != null) {
            productResponse.getData().forEach(dto -> {
                KongProduct product = productMapper.toEntity(dto);
                productRepository.save(product);
            });
        }

        return productResponse;
    } catch (Exception e) {
        throw new RuntimeException("Error while fetching products: " + e.getMessage(), e);
    }
}

// inside ExternalApiServiceImpl

@Override
public List<KongProductResponse> fetchProductsFromDb(Long clientDetailsId) {
    ClientApiDetails details = clientApiDetailsRepository.findById(clientDetailsId)
        .orElseThrow(() -> new RuntimeException("ClientApiDetails not found for id: " + clientDetailsId));

    String url = buildUrl(details.getBaseUrl(), details.getEndpoint());
    System.out.println("👉 Calling Kong URL: " + url);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(details.getAuthToken());     // PAT in DB must be raw, no "Bearer "
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    HttpEntity<String> entity = new HttpEntity<>(headers);

    // ---- FIX: wrap readValue in try/catch ----
    KongProductResponse productResponse;
    try {
        ResponseEntity<String> resp =
    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

String body = resp.getBody();
System.out.println("Konnect raw body:\n" + body); // <-- crucial for debugging

// Defensive parse: don't bind the whole thing at once
com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);

KongProductResponse tmp = new KongProductResponse();
java.util.List<KongProductDTO> items = new java.util.ArrayList<>();

com.fasterxml.jackson.databind.JsonNode data = root.get("data");
if (data != null && data.isArray()) {
    for (com.fasterxml.jackson.databind.JsonNode n : data) {
        KongProductDTO dto = objectMapper.treeToValue(n, KongProductDTO.class);
        items.add(dto);
    }
}
tmp.setData(items);

// keep meta generic (structure can vary)
com.fasterxml.jackson.databind.JsonNode meta = root.get("meta");
if (meta != null && !meta.isNull()) {
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> metaMap =
            objectMapper.convertValue(meta, java.util.Map.class);
    tmp.setMeta(metaMap);
}

productResponse = tmp;

    } catch (Exception e) {
        throw new RuntimeException("Failed to parse Konnect response", e);
    }
    // -----------------------------------------

    if (productResponse.getData() != null) {
        for (KongProductDTO dto : productResponse.getData()) {
            KongProduct p = productMapper.toEntity(dto); // mapper writes labels JSON string
            productRepository.save(p);
        }
    }
    return List.of(productResponse);
}

    @Override
    public List<KongProduct> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public KongProduct getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KongProduct not found for ID: " + id));
    }

    @Override
    public void deleteProductById(String id) {
        productRepository.deleteById(id);
    }

    private String buildUrl(String baseUrl, String endpoint) {
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpoint;
        } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        }
        return baseUrl + endpoint;
    }
    
}
