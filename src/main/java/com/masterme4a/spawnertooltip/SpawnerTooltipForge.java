package com.masterme4a.spawnertooltip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod("spawnertooltip")
public class SpawnerTooltipForge {
    public static final String MOD_ID = "spawnertooltip";

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            CompoundNBT tag = event.getItemStack().getTag();
            if (tag == null || !tag.contains("gs_marker")) return;

            event.getToolTip().add(new StringTextComponent(" "));
            boolean farmerSpawner = tag.contains("gs_sword_item") && isFarmerSword(tag.getString("gs_sword_item"));
            event.getToolTip().add(new StringTextComponent(farmerSpawner ? "✦ ФАРМЕРСКИЙ СПАВНЕР ✦" : "Данные золотого спавнера")
                    .setStyle(Style.EMPTY.applyFormatting(farmerSpawner ? TextFormatting.YELLOW : TextFormatting.AQUA)
                            .applyFormatting(TextFormatting.BOLD)));

            if (tag.contains("gs_sword_item")) appendSword(tag.getString("gs_sword_item"), event);

            long total = tag.getLong("gs_kills_total");
            long remaining = tag.getLong("gs_kills_remaining");
            if (total > 0) {
                long killed = Math.max(0L, Math.min(total, total - remaining));
                int percent = (int) Math.round(killed * 100.0 / total);
                event.getToolTip().add(label("Убито мобов: ", killed + " / " + total + " (" + percent + "%)", farmerSpawner));
            }
            if (tag.contains("gs_mob_type"))
                event.getToolTip().add(label("Тип моба: ", mobName(tag.getString("gs_mob_type")), farmerSpawner));
            if (tag.contains("gs_total_eggs"))
                event.getToolTip().add(label("Вставлено яиц: ", String.valueOf(tag.getLong("gs_total_eggs")), farmerSpawner));
            if (tag.contains("gs_storage_items")) appendStorage(tag.getString("gs_storage_items"), event, farmerSpawner);
        }

        private static IFormattableTextComponent label(String label, String value, boolean farmer) {
            TextFormatting labelColor = farmer ? TextFormatting.GOLD : TextFormatting.GRAY;
            TextFormatting valueColor = farmer ? TextFormatting.YELLOW : TextFormatting.WHITE;
            return new StringTextComponent(label).setStyle(Style.EMPTY.applyFormatting(labelColor))
                    .appendSibling(new StringTextComponent(value).setStyle(Style.EMPTY.applyFormatting(valueColor)));
        }

        private static boolean isFarmerSword(String json) {
            try {
                JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
                if (!obj.has("enchants") || !obj.get("enchants").isJsonObject()) return false;
                for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("enchants").entrySet()) {
                    String key = entry.getKey().toLowerCase();
                    if (key.contains("mob-farmer-enchant") || key.contains("farmer")) return true;
                }
            } catch (Exception ignored) { }
            return false;
        }

        private static void appendSword(String json, ItemTooltipEvent event) {
            try {
                JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
                String name = getString(obj, "name", "Неизвестный меч");
                String type = getString(obj, "type", "");
                boolean farmer = isFarmerSword(json);
                event.getToolTip().add(new StringTextComponent(farmer ? "✦ ФАРМЕР: " : "Меч: ")
                        .setStyle(Style.EMPTY.applyFormatting(farmer ? TextFormatting.GOLD : TextFormatting.GRAY)
                                .applyFormatting(farmer ? TextFormatting.BOLD : TextFormatting.RESET))
                        .appendSibling(new StringTextComponent(name)
                                .setStyle(Style.EMPTY.applyFormatting(farmer ? TextFormatting.YELLOW : TextFormatting.AQUA)
                                        .applyFormatting(farmer ? TextFormatting.BOLD : TextFormatting.RESET))));
                if (!type.isEmpty())
                    event.getToolTip().add(new StringTextComponent("Тип: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.DARK_GRAY))
                            .appendSibling(new StringTextComponent(swordType(type)).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE))));

                if (obj.has("enchants") && obj.get("enchants").isJsonObject()) {
                    JsonObject enchants = obj.getAsJsonObject("enchants");
                    if (!enchants.entrySet().isEmpty()) {
                        event.getToolTip().add(new StringTextComponent("Чары:").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
                        for (Map.Entry<String, JsonElement> entry : enchants.entrySet()) {
                            String key = entry.getKey();
                            if (key.contains(":")) key = key.substring(key.indexOf(':') + 1);
                            String level = entry.getValue().isJsonPrimitive()
                                    ? entry.getValue().getAsString() : entry.getValue().toString();
                            boolean farmerEnchant = key.toLowerCase().contains("mob-farmer-enchant") || key.toLowerCase().contains("farmer");
                            event.getToolTip().add(new StringTextComponent(farmerEnchant ? " ✦ " + enchantName(key) + " " + level + " ✦" : " • " + enchantName(key) + " " + level)
                                    .setStyle(Style.EMPTY.applyFormatting(TextFormatting.YELLOW)
                                            .applyFormatting(farmerEnchant ? TextFormatting.BOLD : TextFormatting.RESET)));
                        }
                    }
                }
            } catch (Exception ignored) {
                event.getToolTip().add(new StringTextComponent("Не удалось прочитать данные меча")
                        .setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED)));
            }
        }

        private static void appendStorage(String json, ItemTooltipEvent event, boolean farmer) {
            try {
                JsonElement root = new JsonParser().parse(json);
                if (!root.isJsonArray()) return;
                event.getToolTip().add(new StringTextComponent(farmer ? "✦ Хранилище фармера:" : "Хранилище:")
                        .setStyle(Style.EMPTY.applyFormatting(farmer ? TextFormatting.GOLD : TextFormatting.GOLD)
                                .applyFormatting(farmer ? TextFormatting.BOLD : TextFormatting.RESET)));
                for (JsonElement el : root.getAsJsonArray()) {
                    if (!el.isJsonObject()) continue;
                    JsonObject item = el.getAsJsonObject();
                    String name = getString(item, "name", "Предмет");
                    String amount = getString(item, "amount", "1");
                    event.getToolTip().add(new StringTextComponent(" • " + name + " x" + amount)
                            .setStyle(Style.EMPTY.applyFormatting(farmer ? TextFormatting.YELLOW : TextFormatting.WHITE)));
                }
            } catch (Exception ignored) { }
        }

        private static String getString(JsonObject obj, String key, String fallback) {
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
        }

        private static String mobName(String id) {
            switch (id.toUpperCase()) {
                case "MAGMA_CUBE": return "Магма-куб";
                case "ZOMBIE": return "Зомби";
                case "SKELETON": return "Скелет";
                case "CREEPER": return "Крипер";
                case "SPIDER": return "Паук";
                case "ENDERMAN": return "Эндермен";
                case "BLAZE": return "Блейз";
                case "WITCH": return "Ведьма";
                case "PIGLIN": return "Пиглин";
                case "WITHER_SKELETON": return "Визер-скелет";
                case "SLIME": return "Слизень";
                case "PHANTOM": return "Фантом";
                case "HOGLIN": return "Хоглин";
                case "EVOKER": return "Вызыватель";
                case "GHAST": return "Гаст";
                case "COW": return "Корова";
                case "PIG": return "Свинья";
                default: return id;
            }
        }

        private static String swordType(String id) {
            switch (id) {
                case "minecraft:wooden_sword": return "Деревянный меч";
                case "minecraft:stone_sword": return "Каменный меч";
                case "minecraft:iron_sword": return "Железный меч";
                case "minecraft:golden_sword": return "Золотой меч";
                case "minecraft:diamond_sword": return "Алмазный меч";
                default: return id;
            }
        }

        private static String enchantName(String key) {
            switch (key) {
                case "sharpness": return "Острота";
                case "smite": return "Небесная кара";
                case "bane_of_arthropods": return "Бич членистоногих";
                case "knockback": return "Отдача";
                case "fire_aspect": return "Заговор огня";
                case "looting": return "Добыча";
                case "sweeping": return "Разящий клинок";
                case "unbreaking": return "Прочность";
                case "mending": return "Починка";
                case "critical-enchant-custom": return "Критический";
                case "destroyer-enchant-custom": return "Разрушитель";
                case "rich-enchant-custom": return "Богач";
                case "mob-farmer-enchant": return "Фармер";
                default: return key;
            }
        }
    }
}
