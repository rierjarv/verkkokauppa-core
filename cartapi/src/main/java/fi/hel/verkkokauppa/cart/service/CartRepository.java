package fi.hel.verkkokauppa.cart.service;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.cart.model.Cart;

@Repository
public interface CartRepository extends CrudRepository<Cart, String> {

    List<Cart> findByNamespaceAndUser(String namespace, String user);

}