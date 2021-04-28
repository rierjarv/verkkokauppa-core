package fi.hel.verkkokauppa.cart.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.cart.model.CartItem;
import fi.hel.verkkokauppa.utils.UUIDGenerator;

@Component
public class CartItemService {
            
    private Logger log = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartItemRepository cartItemRepository;


    public void addItem(String cartId, String productId) {
        String cartItemId = UUIDGenerator.generateType3UUIDString(cartId, productId);

        CartItem cartItem = new CartItem(cartItemId, cartId, productId, 1, "pcs");
        cartItemRepository.save(cartItem);
        log.debug("created new cartItem, cartItemId: " + cartItemId);
    }

    public void removeItem(String cartId, String productId) {
        String cartItemId = UUIDGenerator.generateType3UUIDString(cartId, productId);
        cartItemRepository.deleteById(cartItemId);
        log.debug("deleted cartItem, cartItemId: " + cartItemId);
    }

    public CartItem findById(String cartItemId) {
        Optional<CartItem> mapping = cartItemRepository.findById(cartItemId);
        
        if (mapping.isPresent())
            return mapping.get();

        log.debug("cartItem not found, cartItemId: " + cartItemId);
        return null;
    }

    public List<CartItem> findByCartId(String cartId) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

        if (cartItems.size() > 0)
            return cartItems;

        log.debug("cartItems not found, cartId: " + cartId);
        return null;
    }

}