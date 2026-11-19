package ariolmc.online.aMCEggVoid.core.mail;

import ariolmc.online.aMCEggVoid.core.MemberList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис почты игроков с двусторонней связью (игрок <-> почта) и сохранением в mail.yml.
 */
public class MailService {

    private final Plugin plugin;
    private final File mailFile;

    private final Map<String, PlayerMail> playerToMail = new LinkedHashMap<>();
    private final Map<PlayerMail, String> mailToPlayer = new IdentityHashMap<>();

    public MailService(Plugin plugin, MemberList memberList) {
        this.plugin = plugin;
        this.mailFile = new File(plugin.getDataFolder(), "mail.yml");
        load();
        ensureRegisteredMembers(memberList);
    }

    public synchronized PlayerMail getOrCreateMail(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }
        String key = playerName.toLowerCase();
        PlayerMail mail = playerToMail.get(key);
        if (mail == null) {
            mail = new PlayerMail(key);
            playerToMail.put(key, mail);
            mailToPlayer.put(mail, key);
            save();
        }
        return mail;
    }

    public synchronized PlayerMail getMail(String playerName) {
        return playerName == null ? null : playerToMail.get(playerName.toLowerCase());
    }

    public synchronized String findPlayerByMail(PlayerMail mail) {
        return mail == null ? null : mailToPlayer.get(mail);
    }

    public synchronized void ensureRegisteredMembers(MemberList memberList) {
        if (memberList == null) {
            return;
        }
        boolean createdAny = false;
        for (String name : memberList.getMemberNames()) {
            String key = name.toLowerCase();
            if (!playerToMail.containsKey(key)) {
                PlayerMail mail = new PlayerMail(key);
                playerToMail.put(key, mail);
                mailToPlayer.put(mail, key);
                createdAny = true;
            }
        }
        if (createdAny) {
            save();
        }
    }

    public void deliverOrStore(Player player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return;
        }
        if (!player.isOnline()) {
            storeInMail(player.getName(), item);
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (!leftover.isEmpty()) {
            for (ItemStack extra : leftover.values()) {
                storeInMail(player.getName(), extra);
            }
            player.sendMessage("§6AMCEggVoid | §eИнвентарь полон! Предмет отправлен на вашу почту (§a/mail§e).");
        }
    }

    public void deliverOrStore(String playerName, ItemStack item) {
        if (playerName == null || item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return;
        }
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null && online.isOnline()) {
            deliverOrStore(online, item);
        } else {
            storeInMail(playerName, item);
        }
    }

    private synchronized void storeInMail(String playerName, ItemStack item) {
        PlayerMail mail = getOrCreateMail(playerName);
        if (mail != null) {
            mail.addItem(item);
            save();
        }
    }

    public enum ClaimSingleResult {
        CLAIMED,
        PARTIAL,
        NO_SPACE,
        EMPTY
    }

    public synchronized ClaimSingleResult claimAt(Player player, int index) {
        if (player == null || !player.isOnline()) {
            return ClaimSingleResult.EMPTY;
        }
        PlayerMail mail = getOrCreateMail(player.getName());
        if (mail == null || index < 0 || index >= mail.size()) {
            return ClaimSingleResult.EMPTY;
        }
        List<ItemStack> items = mail.getMutableItems();
        ItemStack item = items.get(index);
        if (item == null || item.getType().isAir()) {
            items.remove(index);
            save();
            return ClaimSingleResult.EMPTY;
        }

        int beforeAmount = item.getAmount();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (leftover.isEmpty()) {
            items.remove(index);
            save();
            return ClaimSingleResult.CLAIMED;
        }

        int leftoverAmount = leftover.values().iterator().next().getAmount();
        if (leftoverAmount < beforeAmount) {
            item.setAmount(leftoverAmount);
            save();
            return ClaimSingleResult.PARTIAL;
        }
        return ClaimSingleResult.NO_SPACE;
    }

    public synchronized int claimAll(Player player) {
        if (player == null || !player.isOnline()) {
            return 0;
        }
        PlayerMail mail = getOrCreateMail(player.getName());
        if (mail == null || mail.isEmpty()) {
            return 0;
        }

        int claimedStacks = 0;
        Iterator<ItemStack> it = mail.getMutableItems().iterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (item == null || item.getType().isAir()) {
                it.remove();
                continue;
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            if (leftover.isEmpty()) {
                it.remove();
                claimedStacks++;
            } else {
                ItemStack remaining = leftover.values().iterator().next();
                item.setAmount(remaining.getAmount());
                break;
            }
        }

        if (claimedStacks > 0) {
            save();
        }
        return claimedStacks;
    }

    public synchronized void load() {
        playerToMail.clear();
        mailToPlayer.clear();

        if (!mailFile.exists()) {
            return;
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(mailFile);
        ConfigurationSection sec = yml.getConfigurationSection("mails");
        if (sec == null) {
            return;
        }

        for (String playerKey : sec.getKeys(false)) {
            String normalized = playerKey.toLowerCase();
            PlayerMail mail = new PlayerMail(normalized);
            List<?> rawList = sec.getList(playerKey);
            if (rawList != null) {
                for (Object obj : rawList) {
                    if (obj instanceof ItemStack item) {
                        mail.addItem(item);
                    }
                }
            }
            playerToMail.put(normalized, mail);
            mailToPlayer.put(mail, normalized);
        }
    }

    public synchronized void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, PlayerMail> entry : playerToMail.entrySet()) {
            yml.set("mails." + entry.getKey(), new ArrayList<>(entry.getValue().getItems()));
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yml.save(mailFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить mail.yml: " + e.getMessage());
        }
    }
}
