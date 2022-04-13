package org.magicat.intuition.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "Journals")
public class Journal {

    @Id
    ObjectId id;

    String title;
    String titleAbbrev;

}
