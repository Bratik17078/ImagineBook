package io.github.jumperonjava.imaginebook.mixin;

import io.github.jumperonjava.imaginebook.*;
import io.github.jumperonjava.imaginebook.image.Image;
import io.github.jumperonjava.imaginebook.image.ImageData;
import io.github.jumperonjava.imaginebook.util.DeletedImageData;
import io.github.jumperonjava.imaginebook.util.BookPaste;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.component.WritableBookContent;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.jumperonjava.imaginebook.image.ImageData.isMouseOverImage;

@Mixin(BookEditScreen.class)
public abstract class NewBookEditScreenMixin extends Screen {

    @Shadow
    private int currentPage;

    @Shadow
    @Final
    private List<String> pages;

    @Shadow
    private PageButton forwardButton;
    @Shadow
    private PageButton backButton;
    @Shadow
    private Component numberOfPages;

    @Shadow
    private MultiLineEditBox page;

    @Shadow
    private void updatePageContent() { throw new AssertionError(); }

    @Shadow
    private void updateButtonVisibility() { throw new AssertionError(); }

    private Component imaginebook$pasteError;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void imaginebook$paste(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!event.isPaste() || !page.isFocused()) return;
        String clipboard = minecraft.keyboardHandler.getClipboard();
        if (clipboard.isEmpty()) return;
        clipboard = StringUtil.filterText(clipboard.replace("\r\n", "\n").replace('\r', '\n'), true);
        var textField = ((MultiLineEditBoxAccessor) page).imaginebook$textField();
        var access = (MultilineTextFieldAccessor) textField;
        int selectionStart = Math.min(textField.cursor(), access.imaginebook$selectionAnchor());
        int selectionEnd = Math.max(textField.cursor(), access.imaginebook$selectionAnchor());
        var planned = BookPaste.plan(page.getValue(), selectionStart, selectionEnd,
                clipboard, WritableBookContent.MAX_PAGES - pages.size() + 1,
                WritableBookContent.PAGE_EDIT_LENGTH, text ->
                        font.getSplitter().splitLines(text, access.imaginebook$width(), Style.EMPTY).size()
                                + (StringUtil.endsWithNewLine(text) ? 1 : 0) <= 14);
        cir.setReturnValue(true);
        if (planned.isEmpty()) {
            imaginebook$pasteError = Component.translatable("imaginebook.error.paste_too_large", WritableBookContent.MAX_PAGES);
            return;
        }
        var result = planned.get();
        pages.remove(currentPage);
        pages.addAll(currentPage, result.pages());
        currentPage += result.cursorPage();
        currentEdited = -1;
        draggedByMouse = -1;
        updatePageContent();
        updateButtonVisibility();
        textField.setSelecting(false);
        textField.seekCursor(Whence.ABSOLUTE, result.cursorOffset());
        imaginebook$pasteError = null;
        updateDisplayImages();
    }

    private EditBox urlField;
    private EditBox xPosField;
    private EditBox widthField;
    private EditBox yPosField;
    private EditBox heightField;
    private EditBox spinField;

    private Button xPosButton;
    private Button yPosButton;
    private Button widthButton;
    private Button heightButton;
    private Button spinButton;

    private Button removeButton;
    private Button addButton;
    private Component error;

    protected NewBookEditScreenMixin(Component title) {
        super(title);
    }

    List<List<ImageData>> display_pages = new ArrayList<>();

    @Inject(method = "<init>", at = @At("TAIL"))

    void construct(Player player, ItemStack stack, InteractionHand hand, WritableBookContent writableBookContent, CallbackInfo ci) {

        for (int i = 0; i < 250; i++) {
            display_pages.add(new ArrayList<>());
        }
        updateDisplayImages();
    }

    void updateDisplayImages() {
        for (int i = 0; i < pages.size(); i++) {
            var page = pages.get(i);
            display_pages.set(i, ImageSerializer.parseSafeModeImages(page));
        }
        currentEdited = Math.min(currentEdited, display_pages.get(currentPage).size() - 1);
    }

    private String getCurrentPageContent() {
        if (currentPage >= pages.size()) {
            return "";
        }
        return pages.get(currentPage);
    }

    private void setCurrentPageContent(String content) {
        pages.set(currentPage, content);

        this.page.setValue(content, true);

    }

    int currentEdited = -1;

    private void setCurrentEdited(int imageId) {
        currentEdited = imageId;
        if (currentEdited == -1) {
            setFocused(null);
        } else {
            updateFields();
        }
    }

    //current page get
    @Nullable
    ImageData getCurrentPageImage(int id) {
        if (currentPage < 0 || currentPage >= display_pages.size() || id < 0 || id >= display_pages.get(currentPage).size())
            return null;
        return display_pages.get(currentPage).get(id);
    }

    void addCurrentPageImage(ImageData image) {
        setCurrentPageContent(getCurrentPageContent() + image.bookString());
        updateDisplayImages();
    }

    @Inject(method = "init", at = @At("TAIL"))
    void init(CallbackInfo ci) {
        int elementHeight = 20;
        int gap = 4;
        int heightOffset = height - 50;
        int columnSize = 100 - gap;
        int smallFieldWidth = columnSize - 24;

        int[] row = new int[10];
        int[] column = new int[10];
        int[] fieldColumn = new int[10];
        for (int i = 0; i < 10; ++i) {
            row[i] = i * (elementHeight + gap) + gap;
        }
        for (int i = 0; i < 10; ++i) {
            column[i] = i * (columnSize + gap) + gap;
        }
        for (int i = 0; i < 10; ++i) {
            fieldColumn[i] = column[i] + 24;
        }

        int urlFieldX = column[0];
        int urlFieldY = row[0];
        int urlFieldWidth = (columnSize) * 2 + gap;

        int addButtonX = column[0];
        int addButtonY = row[1];
        int addButtonWidth = columnSize;

        int removeButtonX = column[1];
        int removeButtonY = row[1];
        int removeButtonWidth = columnSize;

        int xPosButtonX = column[2];
        int xPosButtonY = row[0];
        int xPosFieldX = fieldColumn[2];
        int xPosFieldY = row[0];

        int yPosButtonX = column[2];
        int yPosButtonY = row[1];
        int yPosFieldX = fieldColumn[2];
        int yPosFieldY = row[1];

        int widthButtonX = column[3];
        int widthButtonY = row[0];
        int widthFieldX = fieldColumn[3];
        int widthFieldY = row[0];

        int heightButtonX = column[3];
        int heightButtonY = row[1];
        int heightFieldX = fieldColumn[3];
        int heightFieldY = row[1];

        int spinButtonX = column[4];
        int spinButtonY = row[1];
        int spinFieldX = fieldColumn[4];
        int spinFieldY = row[1];

        urlFieldY += heightOffset;
        addButtonY += heightOffset;
        removeButtonY += heightOffset;
        xPosButtonY += heightOffset;
        xPosFieldY += heightOffset;
        yPosButtonY += heightOffset;
        yPosFieldY += heightOffset;
        widthButtonY += heightOffset;
        widthFieldY += heightOffset;
        heightButtonY += heightOffset;
        heightFieldY += heightOffset;
        spinButtonY += heightOffset;
        spinFieldY += heightOffset;

        urlField = addRenderableWidget(new EditBox(minecraft.font, urlFieldX, urlFieldY, urlFieldWidth, elementHeight, Component.translatable("imaginebook.gui.urlhere")));
        urlField.setMaxLength(256);
        urlField.setResponder((url) -> {
            var getCurrentEdited = getCurrentEdited();
            if(getCurrentEdited == null){
                return;
            }
            if (url.equals(getCurrentEdited.getUrl())) return;
            if (url.length() > 255) {
                urlField.setValue(I18n.get("imaginebook.error.too_long"));
                return;
            }
            mutateImage(currentEdited, (imageData -> {
                imageData.heightFraction = 1;
                imageData.widthFraction = 1;
                imageData.setUrl(Imaginebook.fixImgurLink(url));
                return imageData;
            }));
        });

        addButton = addRenderableWidget(Button.builder(Component.translatable("imaginebook.gui.add"), (b) -> {
            var newImage = new ImageData("", (short) 0, (short) 0, 1, 1);
            addCurrentPageImage(newImage);
            setCurrentEdited(display_pages.get(currentPage).size() - 1);
        }).pos(addButtonX, addButtonY).size(addButtonWidth, elementHeight).build());

        removeButton = addRenderableWidget(Button.builder(Component.translatable("imaginebook.gui.remove"), (b) -> {
            mutateImage(currentEdited, (imageData -> {
                setCurrentEdited(-1);
                return new DeletedImageData();
            }));
        }).pos(removeButtonX, removeButtonY).size(removeButtonWidth, elementHeight).build());

        xPosButton = addRenderableWidget(Button.builder(Component.literal("⇄"), (b) -> {
        }).pos(xPosButtonX, xPosButtonY).size(20, elementHeight).build());
        xPosField = addRenderableWidget(new EditBox(minecraft.font, xPosFieldX, xPosFieldY, smallFieldWidth, elementHeight, Component.empty()));
        xPosField.setResponder(createSetter(x -> {
            mutateImage(currentEdited, (imageData -> {
                imageData.x = x;
                return imageData;
            }));
        }, xPosField));

        yPosButton = addRenderableWidget(Button.builder(Component.literal("⇅"), (b) -> {
        }).pos(yPosButtonX, yPosButtonY).size(20, elementHeight).build());
        yPosField = addRenderableWidget(new EditBox(minecraft.font, yPosFieldX, yPosFieldY, smallFieldWidth, elementHeight, Component.empty()));
        yPosField.setResponder(createSetter(y -> {
            mutateImage(currentEdited, (imageData -> {
                imageData.y = y;
                return imageData;
            }));
        }, yPosField));

        widthButton = addRenderableWidget(Button.builder(Component.literal("⇔"), (b) -> {
        }).pos(widthButtonX, widthButtonY).size(20, elementHeight).build());
        widthField = addRenderableWidget(new EditBox(minecraft.font, widthFieldX, widthFieldY, smallFieldWidth, elementHeight, Component.empty()));
        widthField.setResponder(createSetter(w -> {
            mutateImage(currentEdited, (imageData -> {
                imageData.widthFraction = (float) Math.round(w) / 100;
                return imageData;
            }));
        }, widthField));

        heightButton = addRenderableWidget(Button.builder(Component.literal("⇕"), (b) -> {
        }).pos(heightButtonX, heightButtonY).size(20, elementHeight).build());
        heightField = addRenderableWidget(new EditBox(minecraft.font, heightFieldX, heightFieldY, smallFieldWidth, elementHeight, Component.empty()));
        heightField.setResponder(createSetter(h -> {
            mutateImage(currentEdited, (imageData -> {
                imageData.heightFraction = (float) Math.round(h) / 100;
                return imageData;
            }));
        }, heightField));

        spinButton = addRenderableWidget(Button.builder(Component.literal("↻"), (b) -> {
        }).pos(spinButtonX, spinButtonY).size(20, elementHeight).build());
        spinField = addRenderableWidget(new EditBox(minecraft.font, spinFieldX, spinFieldY, smallFieldWidth, elementHeight, Component.empty()));
        spinField.setResponder(createSetter(h -> {
            mutateImage(currentEdited, (imageData -> {
                imageData.rotation = 0;
                imageData.rotate(h);
                return imageData;
            }));
        }, spinField));

        removeButton.visible = false;
        urlField.setVisible(false);
        xPosField.setVisible(false);
        xPosButton.visible = false;
        yPosField.setVisible(false);
        yPosButton.visible = false;
        widthField.setVisible(false);
        widthButton.visible = false;
        heightField.setVisible(false);
        heightButton.visible = false;
        spinField.setVisible(false);
        spinButton.visible = false;

        updateFields();
    }

    private ImageData getCurrentEdited() {
        return getCurrentPageImage(currentEdited);
    }

    private void updateFields() {
        List<Integer> pageErrors = new ArrayList<>();
        if (pageErrors.size() > 0) {
                        Imaginebook.cancelledFinalize = true;
            error = Component.translatable("imaginebook.error.too_long_encoded", String.join(", ", pageErrors.stream().map(s -> String.valueOf(s + 1)).toArray(String[]::new)));
        } else {
            Imaginebook.cancelledFinalize = false;
            error = Component.empty();
        }

        if (currentEdited == -1)
            return;

        var currentPageImage = getCurrentEdited();

        if (currentPageImage != null) {
            urlField.setValue(currentPageImage.getUrl());

            String xpos = String.valueOf((int) currentPageImage.x());
            xPosField.setValue(xpos);

            String ypos = String.valueOf((int) currentPageImage.y());
            yPosField.setValue(ypos);

            String width = String.valueOf(currentPageImage.widthFraction() * 100);
            widthField.setValue(width);

            String height = String.valueOf(currentPageImage.heightFraction() * 100);
            heightField.setValue(height);

            spinField.setValue(currentPageImage.rotation + "");
        }
    }

    Consumer<String> createSetter(Consumer<Float> setter, EditBox fieldWidget) {
        return (value) -> {
            try {
                float number = Float.parseFloat(value);
                if (String.valueOf(Math.round(number)).equals("0")) {
                    setter.accept(0f);
                } else if (!fieldWidget.getValue().equals(String.valueOf(Math.round(number)))) {
                    fieldWidget.setValue(String.valueOf(Math.round(number)));
                } else {
                    setter.accept(number);
                }
            } catch (NumberFormatException e) {

            }
        };
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {

        var images = display_pages.get(currentPage);

        boolean inSigningScreen = minecraft.gui.screen() instanceof BookSignScreen;
        if (inSigningScreen)
            return false;
        var buttons = new Button[]{forwardButton, backButton};

        for (var button : buttons) {

            if (button.isMouseOver(click.x(), click.y())) {
                button.onPress(click);

                return true;

            }
        }
        int i = 0;
        for (var image : images) {

            if (isMouseOver(image, click.x(), click.y())) {

                setCurrentEdited(i);
                draggedByMouse = i;

                return true;

            }
            i++;
        }

        //Workaround so if you click on book area it unfocuses elements like text fields

        if (isMouseOver(new ImageData("", (short) 0, (short) 0, (short) 192, (short) 192), click.x(), click.y())) {

            setFocused(null);
        }

        return super.mouseClicked(click, doubled);

    }

    public int draggedByMouse = -1;
    public double bufferX, bufferY;

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double deltaX, double deltaY) {

        if (click.button() != 0)

            return false;

        if (draggedByMouse != -1) {
            bufferX += deltaX;
            bufferY += deltaY;
            mutateImage(draggedByMouse, (imageData -> {
                imageData.x += (int) bufferX;
                imageData.y += (int) bufferY;
                return imageData;
            }));
            bufferX -= (int) bufferX;
            bufferY -= (int) bufferY;

            updateFields();
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);

    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) {

        if (draggedByMouse != -1) {
            updateFields();
            draggedByMouse = -1;
        }

        return super.mouseReleased(click);

    }

    void mutateImage(int id, Function<ImageData, ImageData> function) {
        if (getCurrentPageImage(id) == null)
            return;
        var original = new ImageData(getCurrentPageImage(id));
        var modifiedImage = function.apply(original);
        Pattern pattern = Pattern.compile("\\[.*?\\]");

        Matcher matcher = pattern.matcher(getCurrentPageContent());

        List<ImageData> images = new ArrayList<>();
        int index = 0;

        var newPage = getCurrentPageContent();
        while (matcher.find()) {
            var start = matcher.start();
            var end = matcher.end();

            if (index == id) {
                var buf = new StringBuffer(newPage).delete(start, end).insert(start, modifiedImage.bookString());
                newPage = buf.toString();
                break;
            }
            index++;
        }
        setCurrentPageContent(newPage);
        updateDisplayImages();
    }

    @Override

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

        boolean inSigningScreen = minecraft.gui.screen() instanceof BookSignScreen;
        if ( !inSigningScreen ) {
            verticalAmount = Math.signum(verticalAmount);

            if (minecraft.hasControlDown()) {

                verticalAmount *= 5;
            }

            if (minecraft.hasShiftDown()) {

                verticalAmount *= 5;
            }

            if (minecraft.hasAltDown()) {

                verticalAmount *= 5;
            }
            boolean pressed = false;
            if (getCurrentEdited() != null) {
                final double finalVerticalAmount = verticalAmount;
                Image.ImageSize currentNativeImage = getCurrentEdited().getImage().getSize();
                if (xPosButton.isMouseOver(mouseX, mouseY) || xPosField.isMouseOver(mouseX, mouseY)) {
                    mutateImage(currentEdited, (imageData -> {
                        imageData.x += (float) finalVerticalAmount;
                        return imageData;
                    }));
                    pressed = true;
                }
                if (yPosButton.isMouseOver(mouseX, mouseY) || yPosField.isMouseOver(mouseX, mouseY)) {
                    mutateImage(currentEdited, (imageData -> {
                        imageData.y += (float) finalVerticalAmount;
                        return imageData;
                    }));
                    pressed = true;
                }
                if (widthButton.isMouseOver(mouseX, mouseY) || widthField.isMouseOver(mouseX, mouseY)) {
                    mutateImage(currentEdited, (imageData -> {
                        imageData.widthFraction += (float) (finalVerticalAmount / currentNativeImage.getWidth());
                        return imageData;
                    }));
                    pressed = true;
                }
                if (heightButton.isMouseOver(mouseX, mouseY) || heightField.isMouseOver(mouseX, mouseY)) {
                    mutateImage(currentEdited, (imageData -> {
                        imageData.heightFraction += (float) (finalVerticalAmount / currentNativeImage.getHeight());
                        return imageData;
                    }));
                    pressed = true;
                }
                if (spinButton.isMouseOver(mouseX, mouseY) || spinField.isMouseOver(mouseX, mouseY)) {
                    if (verticalAmount == 25)
                        verticalAmount = 15;
                    if (verticalAmount == 125)
                        verticalAmount = 90;
                    if (verticalAmount == -25)
                        verticalAmount = -15;
                    if (verticalAmount == -125)
                        verticalAmount = -90;

                    mutateImage(currentEdited, (imageData -> {
                        imageData.rotate((float) finalVerticalAmount);
                        return imageData;
                    }));
                    pressed = true;
                }
            }

            if (!pressed) {
                int targetImage1 = -1;
                int i = 0;
                for (ImageData image : display_pages.get(currentPage)) {
                    if (isMouseOver(image, mouseX, mouseY)) {
                        if (targetImage1 == -1) {
                            targetImage1 = i;
                            break;
                        }
                    }
                    i++;
                }
                if (draggedByMouse != -1)
                    targetImage1 = draggedByMouse;

                final double finalVerticalAmount = verticalAmount;
                mutateImage(targetImage1, targetImage -> {
                    var localVerticalAmount = finalVerticalAmount;
                    if (targetImage != null) {

                        if (minecraft.hasShiftDown() || minecraft.hasAltDown()) {

                            localVerticalAmount /= 5;
                        }
                        double pow = Math.pow(1.00 + Math.abs(localVerticalAmount * 0.01), Math.signum(localVerticalAmount));

                        var min = Math.min(targetImage.renderWidth(), targetImage.renderHeight());
                        if (Math.abs(min - min * pow) < 1) {
                            pow = (min + localVerticalAmount) / min;
                        }

                        if (!minecraft.hasAltDown()) {

                            targetImage.widthFraction *= pow;
                        }

                        if (!minecraft.hasShiftDown()) {

                            targetImage.heightFraction *= pow;
                        }

                    }
                    return targetImage;
                });

            }
            updateFields();
            if (pressed) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                updateFields();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void imaginebook$render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (imaginebook$pasteError != null) {
            context.centeredText(font, imaginebook$pasteError, width / 2, height - 64, 0xffff6464);
        }
        updateDisplayImages();
        if (currentPage < 0 || currentPage >= display_pages.size()) return;
        boolean editing = getCurrentEdited() != null;
        removeButton.visible = editing;
        for (var field : List.of(urlField, xPosField, yPosField, widthField, heightField, spinField)) {
            field.setVisible(editing);
        }
        for (var button : List.of(xPosButton, yPosButton, widthButton, heightButton, spinButton)) {
            button.visible = editing;
        }
        int bookX = (width - 192) / 2;
        for (ImageData image : display_pages.get(currentPage).reversed()) {
            image.renderImage(context, bookX, 2);
        }
        for (ImageData image : display_pages.get(currentPage)) {
            if (isMouseOver(image, mouseX, mouseY)) {
                context.fill((int) (bookX + image.x()), (int) (2 + image.y()),
                        (int) (bookX + image.x() + image.renderWidth()),
                        (int) (2 + image.y() + image.renderHeight()), 0x0fffffff);
                break;
            }
        }
    }

    public boolean isMouseOver(ImageData imageData, double mouseX, double mouseY) {
        return isMouseOverImage(imageData, mouseX, mouseY, this.width / 2);
    }

    @Inject(method = "saveChanges", at = @At("HEAD"), cancellable = true)
    void finalizeBook(  CallbackInfo ci) {

        if (Imaginebook.cancelledFinalize) {
            ci.cancel();
            return;
        }
    }
}
