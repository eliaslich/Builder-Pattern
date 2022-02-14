package Ingredient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor
@SuppressWarnings("unused")
public class BuildIngredient {

    private final List<Ingredient> parsedIngredients = Lists.newArrayList();
    private final List<String> unparseableLines = Lists.newArrayList();
    private final List<String> ingredientLinesToParse = Lists.newArrayList();
    private String lastIngredientTag=null;
    private final static List<String> SAFE_WORDS = ImmutableList.of("canned");

    private static final Logger log = LoggerFactory.getLogger(BuildIngredient.class);


    public BuildIngredient addIngredientLines(List<String> ingredientsCopy) {
        this.ingredientLinesToParse.addAll(ingredientsCopy);
        return this;
    }

    public BuildIngredient addIngredients(List<Ingredient> ingredients) {
        if(ingredients != null) {
            ingredients.forEach(i -> ingredientLinesToParse.add(i.getIngredientDisplay()));
        }
        return this;
    }

    public List<Ingredient> build() {
        if(unparseableLines.size() > 0) {
            log.info(String.format("parse error, ingredient=%s", unparseableLines));
        }
        ingredientLinesToParse.forEach(this::ParseIngredient);
        return ImmutableList.copyOf(parsedIngredients);
    }



    public List<String> getUnparseableLines() {
        return unparseableLines;
    }

    public List<Ingredient> getParsedIngredients() {
        return parsedIngredients;
    }

    void safeRemoveIngredient(@NotNull List<String> from, String toRemove) {
        boolean removed = false;
        List <String> listToRemove = List.of(toRemove.split(" "));
        for(String toRemoveString: listToRemove){
            if(from.size() > 0
                    && toRemoveString != null
                    && !SAFE_WORDS.contains(toRemoveString)) {
                from.remove(toRemoveString);
            }
        }
    }


    private final static List<String> NEW_EXCLUDE_WORDS = ImmutableList.of("juice of", "juice from", "crushed", "heaped", "Shredded", "cut in half",
            "Sliced", "toasted", "organic", "finely", "finely", "chopped", "Washed", "Halved", "Minced", "Cut", "chunks", "chunk",
            "Thinly sliced", "Roughly chopped",  "Chopped", "sliced", "rinsed", "in half", "Cubed", "Steamed", "Spiralized", "Ribboned",
            "Deseeded", "for serving", "for frying", "dried", "grated", "packed", "for cooking", "for serving", "ready to eat", "into ", " bite ", "size",
            "of ", "(optional)", "(for frying)", "optional", "about", "ripe", "or ", "cubes", "frozen", "possible", "possibly", "one ", "for "); //deleted "pure" since it ruined puree, added "one", "bite", "chunk(s)", "into", "size"

    private final static List<String> PER_QUANTITY_WORDS = ImmutableList.of("each", "per", "á", "à", "correspond", "corresponds");

    /**
     * Parse the whole ingredient line
     * @param ingredientDisplayLine string
     * Create Ingredient object
     */
    void ParseIngredient(final String ingredientDisplayLine){
        // 0. discard lines like "For the sauce:"
        Optional <String> ingredientTag = parseFoodTag(ingredientDisplayLine);
        if (ingredientTag.isPresent()){
            return;
        }

        // 1. remove exclude words
        String ingredientLineData = ingredientDisplayLine.toLowerCase();
        for (String EXCLUDE_WORD : NEW_EXCLUDE_WORDS) {
            ingredientLineData= ingredientLineData.replace(EXCLUDE_WORD.toLowerCase(), "");
        }
        ingredientLineData = ingredientLineData.replaceAll(",", "").trim(). replaceAll("  ", " ");

        // 2. split
        final List<String> ingredientRowData =  Arrays.stream(ingredientLineData.toLowerCase().split(" "))
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        String remainingDisplayLine = ingredientRowData.toString().replaceAll(",","").replace("[", "").replace("]", "");
        final List<String> remainingData = Lists.newArrayList(ingredientRowData);

//        1. Get quantity, parse
        float quantity=1f; //default
        Optional <Float> quantityFound = ParseQuantity(remainingDisplayLine, remainingData);
        if(quantityFound.isPresent()){
            quantity = quantityFound.get();
        }

//        2. Get unit, parse
        MeasurementUnit unit =null; //change to some kind of default
        Optional <MeasurementUnit> unitFound = ParseUnit(remainingDisplayLine, remainingData, false) ;
        if(unitFound.isPresent()){
            unit = unitFound.get();
        }

//        3. Parse food, instantiate ingredient
        Ingredient ingredient;
        String ingredientFoodParsedString = parseIngredientCopy(remainingData);
        if(ingredientDisplayLine.length() > 0) {
            ingredient = new Ingredient(ingredientFoodParsedString, quantity, unit, ingredientDisplayLine, lastIngredientTag);
            parsedIngredients.add(ingredient);
        }
    }


    /**
     * Parse the food tag for first and coming ingredients
     * @param toParse string
     * @return food tag
     */
    Optional <String> parseFoodTag(String toParse){
        boolean foodTagCalled=false;
        if(toParse.endsWith(":")) {
            unparseableLines.add(toParse);
            lastIngredientTag= toParse.replace(":", "");
            foodTagCalled=true;
        }else if(toParse.startsWith("#")){
            unparseableLines.add(toParse);
            lastIngredientTag=toParse.replace("#", "");
            foodTagCalled=true;
        }
        String foodTag = lastIngredientTag;
        return foodTagCalled? Optional.of(foodTag) : Optional.empty();
    }

    /**
     * Parse the quantity number, range quantity, fraction or {@link AmountWord}
     * @param toParse string
     * @return quantity
     */
    Optional <Float> ParseQuantity(String toParse, List <String> remainingData) {

        boolean parsed = false;
        float quantity = 0f;
        Pattern pattern;
        Matcher matcher;

        //Check for brackets
        //Check if bracketQuantity should overwrite quantity completely
        //Check if bracketQuantity should be per quantity (fix at end)
        //parse brackets
        boolean isBracket = false;
        boolean isPerQuantity = false;
        boolean isBracketQuantity = false;
        float bracketQuantity = 0f;

        if (toParse.contains("(")) {
            isBracket = true;
            List<String> bracketStringList = List.of(toParse.split("\\("));
            List<String> bracketStringList2 = List.of(bracketStringList.get(1).split("\\)"));
            String bracketStringToParse = bracketStringList2.get(0);

            for (String PER_QUANTITY_WORD : PER_QUANTITY_WORDS) {
                if (bracketStringToParse.contains(PER_QUANTITY_WORD)) {
                    isPerQuantity = true;
                    break;
                }
            }
            //Call itself recursively and send in only the parts within the brackets
            Optional<Float> quantityFound = ParseQuantity(bracketStringToParse, remainingData);
            if (quantityFound.isPresent()) {
                bracketQuantity = quantityFound.get();
                isBracketQuantity = true;
            }
            //Remove the brackets
            toParse = toParse.replaceFirst("(" + bracketStringToParse + ")", "");
            safeRemoveIngredient(remainingData, "(" + bracketStringToParse + ")");
        }

        //Check for range words
        String RangeRegEx = "\\d+?\\s?-\\s?\\d+?";     //"3 - 4"
        pattern = Pattern.compile(RangeRegEx);
        matcher = pattern.matcher(toParse);
        boolean rangeMatch = matcher.find();
        if(rangeMatch) {
            String[] range = matcher.group().split("-");
            int rangeOne = Integer.parseInt(range[0].strip());
            int rangeTwo = Integer.parseInt(range[1].strip());
            safeRemoveIngredient(remainingData, matcher.group(0));
            quantity = (rangeOne + rangeTwo) / 2.0f;
            parsed = true;
        }

        //Check for amount words
        Optional<AmountWord> amountWord = AmountWord.find(toParse);
        float amountWordQuantity =0f;
        String fractionRegEx = "(\\s?\\d/\\d)";     //normal fractions regular expression
        boolean amountParsed = false;
        String vulgarFractionRegEx ="\\d?[\\u00BC-\\u00BE\\u2150-\\u215E]"; //vulgar fraction regular expression
        if(amountWord.isPresent()) {
            amountWordQuantity = amountWord.get().getFloatValue();
            pattern = Pattern.compile(fractionRegEx);
            matcher = pattern.matcher(toParse);
            Pattern vulgarPattern = Pattern.compile(vulgarFractionRegEx);
            Matcher vulgarMatcher = vulgarPattern.matcher(toParse);
            amountParsed=true;
            if (matcher.find()){
                safeRemoveIngredient(remainingData, matcher.group(0));
                toParse= toParse.replaceFirst(fractionRegEx, "");
            }else if(vulgarMatcher.find()){
                safeRemoveIngredient(remainingData, vulgarMatcher.group(0));
                toParse= toParse.replaceFirst(vulgarFractionRegEx, "");

            }
        }

        //Check for quantity if no edge:
        String quantityRegEx = "(\\d+)\\s?\\.?(\\d+)?";
        pattern = Pattern.compile(quantityRegEx);
        matcher = pattern.matcher(toParse);
        boolean quantityMatch = matcher.find();
        if(quantityMatch && !parsed) {
            quantity = Float.parseFloat(matcher.group(0));
            parsed = true;
            safeRemoveIngredient(remainingData, matcher.group(0));
            toParse= toParse.replaceFirst(quantityRegEx, "");
        }

        if (amountParsed) parsed=true;
        quantity += amountWordQuantity;

        //if brackets change quantity if necessary
        if(isBracket){
            if(isPerQuantity){
                quantity = quantity*bracketQuantity;
            }
            else if(isBracketQuantity){
                quantity=bracketQuantity;
            }
        }

        if(!parsed) {
            log.debug(String.format("unable to parse quantity in %s", toParse));
        }

        return (parsed) ? Optional.of(quantity) : Optional.empty();
    }


    /**
     * Parse the unit
     * @param toParse string, remainingData List <String>, recursive boolean
     * @return unit
     */
    Optional <MeasurementUnit> ParseUnit(String toParse, List <String> remainingData, boolean recursive){
        boolean parsed = false;
        MeasurementUnit unit = null;
        List <String> toParseUnit = List.of(toParse.split(" ")); //typically, first index being quantity

        for (String unitParse: toParseUnit){
            Optional <MeasurementUnit> unitFound = MeasurementUnit.parseUnit(unitParse);
            if (unitFound.isPresent()){
                unit = unitFound.get();
                if(!recursive) safeRemoveIngredient(remainingData,unitParse);
                parsed=true;
                break;
            }
        }
        MeasurementUnit bracketUnit=null;
        if (toParse.contains("(")){
            List<String> bracketStringList = List.of(toParse.split("\\("));
            List<String> bracketStringList2 = List.of(bracketStringList.get(1).split("\\)"));
            String bracketStringToParse = bracketStringList2.get(0);
            Optional <MeasurementUnit> unitFound = ParseUnit(bracketStringToParse, remainingData, true);
            if (unitFound.isPresent()){
                unit = unitFound.get();
                parsed=true;
            }
        }
        return (parsed) ? Optional.of(unit) : Optional.empty();
    }


    /**
     * Parse the quantity and unit
     * For future improvement to parsing
     * @param toParse string
     */
    Optional<Pair<Float, MeasurementUnit>> getQuantityMixedIn(String toParse) {
        float quantity = 0f;
        MeasurementUnit unit = null;
        boolean mixInFound = false;
        // check for quantity mixed in with units 300g
        boolean gMixIn = toParse.matches("(\\d+)(\\s?)g");
        if(gMixIn) {
            quantity = Integer.parseInt(toParse.substring(0, toParse.length()-1).replaceAll("\\W", ""));
            unit = MeasurementUnit.g;
            mixInFound = true;
        }
        boolean kgMixIn = toParse.matches("(\\d+)(\\s?)kg");
        if(kgMixIn) {
            quantity = Integer.parseInt(toParse.substring(0, toParse.length()-2).replaceAll("\\W", ""));
            unit = MeasurementUnit.kg;
            mixInFound = true;
        }
        boolean mlMixIn = toParse.matches("(\\d+)(\\s?)ml");
        if(mlMixIn) {
            quantity = Integer.parseInt(toParse.substring(0, toParse.length()-2).replaceAll("\\W", ""));
            unit = MeasurementUnit.ml;
            mixInFound = true;
        }
        boolean tspMixIn = toParse.matches("(\\d+)(\\s?)tsp");
        if(tspMixIn) {
            quantity = Integer.parseInt(toParse.substring(0, toParse.length()-3).replaceAll("\\W", ""));
            unit = MeasurementUnit.tsp;
            mixInFound = true;
        }
        boolean tbspMixIn = toParse.matches("(\\d+)(\\s?)tbsp");
        if(tbspMixIn) {
            quantity = Integer.parseInt(toParse.substring(0, toParse.length()-4).replaceAll("\\W", ""));
            unit = MeasurementUnit.tbsp;
            mixInFound = true;
        }

        return mixInFound ? Optional.of(Pair.of(quantity, unit)) : Optional.empty();
    }


    /**
     * This method is called (hopefully) after the amount and measure unit has been extracted, excluded words have been removed.
     * The rest of the stream is the ingredient copy
     * @param remainingData List <String>
     * @return ingredientCopy
     */
    String parseIngredientCopy(List<String> remainingData) {
        StringBuilder foodBuilder = new StringBuilder();
        String ingredientCopy = "";
        for (String datum : remainingData) {
            if (datum.length() > 0 && ',' == datum.charAt(datum.length() - 1)) {
                foodBuilder.append(datum);
                break;
            }
            foodBuilder.append(datum).append(" ");
        }
        // set ingredientCopy with the remaining food string
        if(foodBuilder.length() > 0) {
            ingredientCopy = foodBuilder.substring(0, foodBuilder.length() - 1).toLowerCase();
        }
        return ingredientCopy;
    }

}
