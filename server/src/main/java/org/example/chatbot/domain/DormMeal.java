package org.example.chatbot.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "dorm_meals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DormMeal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mealDate;

    private String mealTime;

    @Column(columnDefinition = "TEXT")
    private String menu;

    @Column(length = 64, unique = true)
    private String hash;

    @Column(name = "formatted_menu", columnDefinition = "TEXT")
    private String formattedMenu;
}