package team.chisel.carving;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Chisel. Минимум для RM/GT_API. */
public class Carving {
    public static Chisel chisel;

    public static class Chisel {
        public void addGroup(Object aGroup) {}

        public Group getGroup(String aName) {
            return null;
        }
    }

    public static class Group {
        public void setOreName(String aOreName) {}

        public void addVariation(Object aVariation) {}
    }
}
