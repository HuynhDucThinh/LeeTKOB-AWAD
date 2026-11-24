package com.leetkob.entities;

import com.leetkob.entities.ids.TaClassId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ta_classes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaClass {

  @EmbeddedId private TaClassId id;
}
