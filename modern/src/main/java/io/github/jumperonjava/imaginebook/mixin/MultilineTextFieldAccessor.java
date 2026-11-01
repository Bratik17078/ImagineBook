package io.github.jumperonjava.imaginebook.mixin;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {
    @Accessor("selectCursor")
    int imaginebook$selectionAnchor();

    @Accessor("width")
    int imaginebook$width();
}
