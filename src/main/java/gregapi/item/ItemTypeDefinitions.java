package gregapi.item;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/** Номера доступны при регистрации, а стеки, рецепты и поведение создаются позднее в исходном порядке. */
public final class ItemTypeDefinitions {
	private final BitSet mTypes = new BitSet();
	private final List<Runnable> mInitializers = new ArrayList<>();
	private int mLastType = -1;

	public void add(int aType, IntConsumer aInitialize) {
		declare(aType);
		then(() -> aInitialize.accept(aType));
	}
	public void addAll(int[] aTypes, Consumer<int[]> aInitialize) {
		for (int tType : aTypes) declare(tType);
		then(() -> aInitialize.accept(aTypes));
	}
	private void declare(int aType) {
		if (aType < 0 || aType >= 32767 || mTypes.get(aType)) throw new IllegalArgumentException("Invalid or duplicate item type: " + aType);
		mTypes.set(aType);
		mLastType = aType;
	}
	public int lastType() {return mLastType;}
	public IntStream types() {return mTypes.stream();}
	public void then(Runnable aInitialize) {mInitializers.add(aInitialize);}
	public void initialize() {
		for (Runnable tInitialize : mInitializers) tInitialize.run();
		mInitializers.clear();
	}
}
