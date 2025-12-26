package de.tkunkel.game.artifactsmmo.shopping;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class Wish implements Comparable<Wish> {
    @Override
    public String toString() {
        return "Wish{" +
                "reservedBy='" + reservedBy + '\'' +
                ", itemCode='" + itemCode + '\'' +
                ", amount=" + amount +
                ", characterName='" + characterName + '\'' +
                ", fulfilled=" + fulfilled +
                '}';
    }

    public String itemCode;
    public int amount;
    public String reservedBy;
    public String characterName;
    public boolean fulfilled;

    public Wish(String characterName, String itemCode, int amount) {
        this.reservedBy = null;
        this.characterName = characterName;
        this.itemCode = itemCode;
        this.amount = amount;
        this.fulfilled = false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Wish wish = (Wish) o;
        return amount == wish.amount && fulfilled == wish.fulfilled && Objects.equals(reservedBy, wish.reservedBy) && Objects.equals(characterName, wish.characterName) && Objects.equals(itemCode, wish.itemCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservedBy, characterName, itemCode, amount, fulfilled);
    }

    @Override
    public int compareTo(@NonNull Wish o) {
        return this.itemCode.compareTo(o.itemCode);
    }
}
