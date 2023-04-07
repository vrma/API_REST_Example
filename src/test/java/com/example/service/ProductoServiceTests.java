package com.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Sort;

import com.example.dao.ProductoDao;
import com.example.entities.Producto;
import com.example.services.ProductoServiceImpl;

// Para seguir el enfoque BDD con Mockito
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class ProductoServiceTests {
    
    @Mock
    private ProductoDao productoDao;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = Producto.builder()
            .id(1L)
            .nombre("Pixel")
            .descripcion("Google Phone")
            .precio(750.0)
            .stock(50)
            .build();
    }

    @DisplayName("Test de servicio para persistir un producto")
    @Test
    void testGuardarProducto() {

        // given
        given(productoDao.save(producto)).willReturn(producto);

        // when
        Producto productoGuardado = productoService.save(producto);

        // then
        assertThat(productoGuardado).isNotNull();

    }

    @DisplayName("Test de recuperacion de lista vacia de productos")
    @Test
    void testListaVaciaProductos() {

        // given
        given(productoDao.findAll()).willReturn(Collections.emptyList());

        // when
        List<Producto> productos = productoDao.findAll(); 

        // then
        assertThat(productos).isEmpty();
    }
}
