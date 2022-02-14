package Ingredient;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.beet.model.foundation.food.HouseholdWeight;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle error
class BuildIngredientTest {

    @Test
    void build() {
    }

    @Test
    void getParsedIngredients() {
    }

    @Test
    void safeRemoveIngredient() {
    }

    @Test
    void parseIngredient() {
    }

    @Test
    void parseFoodTag() {
    }

    @Test
    void parseQuantity() {
    }

    @Test
    void parseUnit() {
        final List<String> remainingData = Lists.newArrayList("300", "ml");
        IngredientsBuilder builder = new IngredientsBuilder();
        Optional<MeasurementUnit> unit = builder.newParseUnit("300 ml", remainingData, false);
        assertTrue(unit.isPresent());
        assertEquals(MeasurementUnit.ml, unit.get());
    }
    }
}