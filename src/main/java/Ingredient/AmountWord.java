package Ingredient;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.Optional;

@Getter
public enum AmountWord {
    one(1.0f,"" , "" ),
    oneAndHalf(1.5f, "1 1/2", "1½"),
    oneAnd34(1.75f, "1 3/4", "1¾"),
    twoAndHalf(2.5f, "2 1/2", "2½"),
    half(0.5f, "1/2", "\u00BD"),
    quarter(0.25f, "1/4", "\u00BC"),
    three_quarter(0.75f, "3/4", "\u00BE"),
    one_third(0.33f, "1/3", "\u2153"),
    two_third(0.66f, "2/3", "\u2154");

    AmountWord(float floatValue, String fractionString, String vulgarFraction) {
        this.floatValue = floatValue;
        this.fractionString = fractionString;
        this.vulgarFraction = vulgarFraction;
    }

    private float floatValue;
    private String fractionString;
    private String vulgarFraction;

    static public Optional<AmountWord> find(String toParse) {
        String[] stringToParse = toParse.split(" ");
        Optional <AmountWord> found = Optional.empty();
        for (String string: stringToParse){
            found = Lists.newArrayList(AmountWord.values())
                    .stream()
                    .filter(amountWord -> {return amountWord.fractionString.equals(string)
                            || amountWord.vulgarFraction.equals(string)
                            || amountWord.name().equals(string.toLowerCase());})
                    .findFirst();
            if (found.isPresent())break;
        }
        return found;
    }

    public float getFloatValue() {
        return this.floatValue;
    }
}
