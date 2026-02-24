package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import es.ucm.fdi.iw.model.Transferable;

@Entity
@Data
@NamedQueries({
})
@Table(name = "Game")
public class Game implements Transferable<Game.Transfer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "gen", sequenceName = "gen")
    private long id;

}