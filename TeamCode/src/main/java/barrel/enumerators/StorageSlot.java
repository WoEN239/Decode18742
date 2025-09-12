package barrel.enumerators;


enum SlotName
{
    LEFT,
    CENTER,
    RIGHT
}

public class StorageSlot
{
    public int LEFT()   { return 0; }
    public int CENTER() { return 1; }
    public int RIGHT()  { return 2; }

    public SlotName ToSlotName (int value)
    {
        return value == 0 ? SlotName.LEFT
                : value == 1 ? SlotName.CENTER
                : SlotName.RIGHT;
    }
    public int ToInt (SlotName slot)
    {
        return slot == SlotName.LEFT ? 0
                : slot == SlotName.CENTER ? 1
                : 2;
    }
}