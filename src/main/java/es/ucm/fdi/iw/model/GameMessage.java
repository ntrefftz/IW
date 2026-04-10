package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
/**
 * Mensaje para comunicacion WebSocket
 */
public class GameMessage {
    private String type;     
    private Object content;  
}