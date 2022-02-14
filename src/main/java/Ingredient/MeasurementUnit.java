package Ingredient;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;

@AllArgsConstructor
public enum MeasurementUnit {
    g(of("g", "g.", "grams", "gram")),
    mg(of("mg", "mg.", "milligrams")),
    kg(of("kg", "kg.", "kilograms")),
    oz(of("oz")),
    lb(of("lb", "lb.", "pound", "pounds")),
    qt(of("qt", "qt.", "quart", "quarts", "qts")),
    c(of("c", "c.", "cup", "cups", "pot", "cup whole kernels", "cup chopped")),
    tbsp(of("tbsp", "tb.", "tbsp.", "tablespoon", "tablespoons", "tbsps", "tbsp, whole")),
    tsp(of("tsp", "t", "t.", "tsp.", "teaspoon", "teaspoons", "tsps", "tsp, whole")),
    krm(of("krm")),
    l(of("l", "litre", "liter", "liters", "lt", "l.")),
    dl(of("dl", "decilitre", "deciliter", "deciliters", "dl.")),
    ml(of("ml", "millilitre", "milliliter", "milliliters", "ml.")),
    in(of("in", "in.", "inch", "inches")),
    ft(of("f")),
    cm(of("cm")),
    m(of("m")),
    pack(ImmutableList.of("pack", "packs", "package", "box", "packet", "packets")),
    piece(ImmutableList.of("pack", "package", "box", "packet")),
    large(of("large")),
    small(of("small", "fruit, small")),
    medium(of("medium", "stalk", "bunch", "sprig", "sprigs", "fruit", "fruit without skin and seeds", "fruit, without refuse", "pepper")),
    canned(of("can", "canned", "can (5 oz)", "jar")),
    can(of("cant")), // deprecate
    slice(of("slice", "slices")),
    pinch(of("pinch")),
    clove(of("clove", "cloves")),
    bunch(of("bunch", "bunches")),
    servings(of("serving", "servings"));

    private List<String> householdNames;

    private static final Logger log = LoggerFactory.getLogger(MeasurementUnit.class);

    <E> MeasurementUnit(ImmutableList<E> of) {
    }

    public static Optional<MeasurementUnit> parseUnit(@NotNull String toParse) {
        Optional<MeasurementUnit> found = null;
        try {
            found = Optional.of(MeasurementUnit.valueOf(toParse));
        } catch (IllegalArgumentException iae) {
            found = Lists.newArrayList(MeasurementUnit.values())
                    .stream()
                    .filter(m -> {return m.householdNames.contains(toParse.toLowerCase());})
                    .findFirst();
        }

        return found;
    }

}
