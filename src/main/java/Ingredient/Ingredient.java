package Ingredient;

public class Ingredient {

    /**
     * The ingredient name
     */
    private String name;

    /**
     * The ingredient amount
     */
    private float quantity;

    /**
     * The ingredient amount
     */
    private MeasurementUnit unit;

    /**
     * The line as shown in the recipe
     */
    private String ingredientDisplayLine;

    /**
     * The tag showing what part of recipe ingredient belongs to
     */
    private String lastIngredientTag;



    public Ingredient(String name, float quantity, MeasurementUnit unit, String ingredientDisplayLine, String lastIngredientTag) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.ingredientDisplayLine = ingredientDisplayLine;
        this.lastIngredientTag = lastIngredientTag;
    }


    public String getIngredientDisplay() {
        return name;
    }

}
