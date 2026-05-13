package org.magicat.model;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Question {
    @JsonView(Views.Question.class)
    private String text;

    @JsonView(Views.Question.class)
    private String question;
}
