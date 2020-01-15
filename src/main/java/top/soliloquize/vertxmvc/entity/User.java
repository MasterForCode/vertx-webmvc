package top.soliloquize.vertxmvc.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * @author wb
 * @date 2019/9/26
 */
@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
