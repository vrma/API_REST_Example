package com.example.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.entities.Producto;
import com.example.model.FileUploadResponse;
import com.example.services.ProductoService;
import com.example.utilities.FileDownloadUtil;
import com.example.utilities.FileUploadUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private FileUploadUtil fileUploadUtil;

    // Se inyectará por constructor
    private final FileDownloadUtil fileDownloadUtil;
    /**
     * 
     * El metodo siguiente va a responder a una peticion (request) del tipo:
     * http://localhost:8080/productos?page=1&size=4,
     * es decir, que tiene que ser capaz de devolver un listado de productos paginados
     * , o no, pero en cualquier caso ordenados por un criterio (nombre, descripcion, etc)
     * 
     * La peticion anterior implica @RequestParam
     * 
     * /productos/3 => @PathVariable
     */
    @GetMapping
    public ResponseEntity<List<Producto>> findAll(@RequestParam( name = "page", required = false) Integer page,
                                                  @RequestParam( name = "size", required = false) Integer size ) {

           ResponseEntity<List<Producto>> responseEntity = null;
           List<Producto> productos = new ArrayList<>();

           Sort sortByNombre = Sort.by("nombre");                                           

           if(page != null && size != null) {

                // Con paginacion y ordenamiento
                try {
                    Pageable pageable = PageRequest.of(page, size, sortByNombre);
                    Page<Producto> productosPaginados = productoService.findAll(pageable);
                    productos = productosPaginados.getContent();
                    responseEntity = new ResponseEntity<List<Producto>>(productos, HttpStatus.OK);
                } catch (Exception e) {
                    responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
           } else {
                // Sin paginacion, pero con ordenamiento
                try {
                    productos = productoService.findAll(sortByNombre);
                    responseEntity = new ResponseEntity<List<Producto>>(productos, HttpStatus.OK);
                } catch (Exception e) {
                    responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);   
                }
           }
           
           return responseEntity; 

    } 


    /**
     * Recupera un producto por el id
     * Va responder a una peticion del tipo, por ejemplo:
     * http://localhost:8080/productos/2
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable(name = "id") Integer id) {

        ResponseEntity<Map<String, Object>> responseEntity = null;
        Map<String, Object> responseAsMap = new HashMap<>(); 

        try {
            Producto producto = productoService.findById(id);

            if( producto != null) {
                String successMessage = "Se ha encontrado el producto con id: " + id;
                responseAsMap.put("mensaje", successMessage);
                responseAsMap.put("producto", producto);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);    
            } else {
                String errorMessage = "No se ha encontrado el producto con id: " + id;
                responseAsMap.put("error", errorMessage);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.NOT_FOUND);
            }

        } catch (DataAccessException e) {
            String errorGrave = "Error grave, y la causa mas probable puede ser: " + e.getMostSpecificCause();
            responseAsMap.put("error", errorGrave);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }



    /**
     * Persiste un producto en la base de datos
     * @throws IOException
     * 
     * */
    // Guardar (Persistir), un producto, con su presentacion en la base de datos
    // Para probarlo con POSTMAN: Body -> form-data -> producto -> CONTENT TYPE ->
    // application/json
    // no se puede dejar el content type en Auto, porque de lo contrario asume
    // application/octet-stream
    // y genera una exception MediaTypeNotSupported
    @PostMapping( consumes = "multipart/form-data" )
    @Transactional
    public ResponseEntity<Map<String, Object>> insert(
        @Valid 
        @RequestPart(name = "producto") Producto producto, 
        BindingResult result,  
        @RequestPart(name = "file") MultipartFile file) throws IOException {

        Map<String, Object> responseAsMap = new HashMap<>();
        ResponseEntity<Map<String, Object>> responseEntity = null;

        /**
         * Primero: Comprobar si hay errores en el producto recibido
         */
        if(result.hasErrors()) {
            List<String>  errorMessages = new ArrayList<>();

            for(ObjectError error : result.getAllErrors()) {
                errorMessages.add(error.getDefaultMessage());
            }

            responseAsMap.put("errores", errorMessages);

            responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.BAD_REQUEST);

            return responseEntity;
        }

        // Si no hay errores, entonces persistimos el producto,
        // comprobando previamente si nos han enviado una imagen
        // , o un archivo.
        if(!file.isEmpty()) {
            String fileCode = fileUploadUtil.saveFile(file.getOriginalFilename(), file);
            producto.setImagenProducto(fileCode+ "-" + file.getOriginalFilename());

            // Devolver informacion respecto al file recibido

            FileUploadResponse fileUploadResponse = FileUploadResponse
                       .builder()
                       .fileName(fileCode + "-" + file.getOriginalFilename())
                       .downloadURI("/productos/downloadFile/" 
                                 + fileCode + "-" + file.getOriginalFilename())
                       .size(file.getSize())
                       .build();
            
            responseAsMap.put("info de la imagen: ", fileUploadResponse);           

        }

        Producto productoDB =  productoService.save(producto);

        try {
            if(productoDB != null) {
                String mensaje = "El producto se ha creado correctamente";
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("producto", productoDB);
                responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.CREATED);
            } else {
                // No se ha creado el producto
            }            
        } catch (DataAccessException e) {
            String errorGrave = "Ha tenido lugar un error grave " 
                                  + ", y la causa mas probable puede ser " +
                                     e.getMostSpecificCause();
            responseAsMap.put("errorGrave", errorGrave);
            responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, 
                              HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }


    /**
     * Actualiza un producto en la base de datos
     * 
     * */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Producto producto, 
                              BindingResult result,
                              @PathVariable(name = "id") Integer id) {

        Map<String, Object> responseAsMap = new HashMap<>();
        ResponseEntity<Map<String, Object>> responseEntity = null;

        /**
         * Primero: Comprobar si hay errores en el producto recibido
         */
        if(result.hasErrors()) {
            List<String>  errorMessages = new ArrayList<>();

            for(ObjectError error : result.getAllErrors()) {
                errorMessages.add(error.getDefaultMessage());
            }

            responseAsMap.put("errores", errorMessages);

            responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.BAD_REQUEST);

            return responseEntity;
        }

        // Si no hay errores, entonces persistimos el producto
        // Vinculando, previamente, el id que se recibe con el producto
        producto.setId(id);
        Producto productoDB =  productoService.save(producto);

        try {
            if(productoDB != null) {
                String mensaje = "El producto se ha actualizado correctamente";
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("producto", productoDB);
                responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, HttpStatus.OK);
            } else {
                // No se ha actualizado el producto
            }            
        } catch (DataAccessException e) {
            String errorGrave = "Ha tenido lugar un error grave " 
                                  + ", y la causa mas probable puede ser " +
                                     e.getMostSpecificCause();
            responseAsMap.put("errorGrave", errorGrave);
            responseEntity = new ResponseEntity<Map<String,Object>>(responseAsMap, 
                              HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }

    /**
     * Eliminar un producto
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<String> delete(@PathVariable(name = "id") Integer id) {

        ResponseEntity<String> responseEntity = null;

        try {
             // Recuperamos el producto
             Producto producto = productoService.findById(id);

             if(producto != null) {
                productoService.delete(producto);
                responseEntity = new ResponseEntity<String>("Borrado exitosamente", HttpStatus.OK);
             } else {
                responseEntity = new ResponseEntity<String>("No existe el producto", HttpStatus.NOT_FOUND);
             }
           
        } catch (DataAccessException e) {
            e.getMostSpecificCause();
            responseEntity = new ResponseEntity<String>("Error fatal", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }


    /**
     *  Implementa filedownnload end point API 
     **/    
    @GetMapping("/downloadFile/{fileCode}")
    public ResponseEntity<?> downloadFile(@PathVariable(name = "fileCode") String fileCode) {

        Resource resource = null;

        try {
            resource = fileDownloadUtil.getFileAsResource(fileCode);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        if (resource == null) {
            return new ResponseEntity<>("File not found ", HttpStatus.NOT_FOUND);
        }

        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
        .body(resource);

    }    

    /**
     * El método siguiente es de ejemplo para entender el formato JSON
     * , no tiene que ver en sí con el proyecto
     */
    // @GetMapping
    // public List<String> nombres() {
    //     List<String> nombres = Arrays.asList(
    //         "Salma", "Judith", "Elisabet"
    //     );
    //     return nombres; 
    // } 

}
