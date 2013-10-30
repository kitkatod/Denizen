package net.aufdemrand.denizen.scripts.commands.player;

import org.bukkit.entity.Player;

import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.objects.aH;
import net.aufdemrand.denizen.utilities.debugging.dB;


public class ExperienceCommand extends AbstractCommand {

    private enum Type { SET, GIVE, TAKE }

    /**
     * with help from author: alkarin
     * https://github.com/alkarinv/BattleArena/blob/master/src/mc/alk/arena/util/ExpUtil.java
     */
    public static int getTotalExperience(Player p){return getTotalExperience(p.getLevel(),p.getExp());}
    public static int getTotalExperience(int level, double bar){return getTotalExpToLevel(level) + (int) (getExpToLevel(level+1)*bar);}
    public static int getExpToLevel(int level) {if (level < 16){return 17;} else if (level < 31){return 3*level - 31;}else {return 7*level - 155;}}
    public static int getTotalExpToLevel(int level){if (level < 16){return 17*level;} else if (level < 31){    return (int) (1.5*level*level -29.5*level+360 );} else {return (int) (3.5*level*level-151.5*level+2220);}}
    public static void setTotalExperience(Player player, int exp){player.setTotalExperience(0);player.setLevel(0);player.setExp(0);if (exp > 0)player.giveExp(exp);}
    public static void setLevel(Player player, int level){player.setTotalExperience(0);player.setLevel(0);player.setExp(0);if (level > 0)player.giveExp(getExpToLevel(level));}
    public static void giveExperience(Player player, int exp){final int currentExp = getTotalExperience(player);player.setTotalExperience(0);player.setLevel(0);player.setExp(0);final int newexp = currentExp + exp;if (newexp > 0)player.giveExp(newexp);}

    /* Tail recursive way to count the level for the given exp, maybe better with iteration */
    public static int countLevel(int exp, int toLevel, int level){if (exp < toLevel){return level;} else {return countLevel(exp - toLevel, getTotalExpToLevel(level+2) - getTotalExpToLevel(level+1), ++level);}}
    /* Setting the new level and exp using the setExp and setLevel methods, should be soundless (not yet tested) */
    public static void setSoundlessTotalExperience(Player player, int exp) {player.setTotalExperience(0);player.setLevel(0);player.setExp(0); if (exp > 0) {final int level = countLevel(exp, 17, 0);player.setLevel(level); player.setExp((getTotalExpToLevel(level)-exp)/getExpToLevel(level+1));}}
    /* Adding experience using the setExp and setLevel methods, should be soundless (not tested) */
    public static void giveSoundlessExperience(Player player, int exp){final int currentExp = getTotalExperience(player);player.setTotalExperience(0);player.setLevel(0);player.setExp(0);final int newexp = currentExp + exp;if (newexp > 0){final int level = countLevel(currentExp + exp, 17, 0);player.setLevel(level); player.setExp((getTotalExpToLevel(level)-exp)/getExpToLevel(level+1));}}

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        int amount = 0;
        Type type = Type.SET;
        boolean level = false;
        boolean soundless = false;

        for (String arg : scriptEntry.getArguments()) {

            if (aH.matchesQuantity(arg) || aH.matchesInteger(arg)) {
                amount = aH.getIntegerFrom(arg);
            }

            else if (aH.matchesArg("SET, GIVE, TAKE", arg))
                type = Type.valueOf(arg.toUpperCase());

            else if(aH.matchesArg("LEVEL", arg))
                level = true;
            
            else if(aH.matchesArg("SOUNDLESS", arg))
                soundless = true;

            else throw new InvalidArgumentsException("Unknown argument '" + arg + "'");
        }

        scriptEntry.addObject("quantity", amount)
                .addObject("type", type)
                .addObject("level", level)
                .addObject("soundless", soundless);

    }


    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        Type type = (Type) scriptEntry.getObject("type");
        Integer quantity = (Integer) scriptEntry.getObject("quantity");
        Boolean level = (Boolean) scriptEntry.getObject("level");
        Boolean sound = !((Boolean) scriptEntry.getObject("soundless"));

        dB.report(scriptEntry, name, aH.debugObj("Type", type.toString())
            + aH.debugObj("Quantity", level ? quantity.toString() + " levels" : quantity.toString())
            + aH.debugObj("Player", scriptEntry.getPlayer().getName()));

        Player player = scriptEntry.getPlayer().getPlayerEntity();

        switch (type) {
            case SET:
                if(level)
                    setLevel(player, quantity);
                else if ( sound )
                    setTotalExperience(player, quantity);
                else
                    setSoundlessTotalExperience(player, quantity);
                break;

            case GIVE:
                if(level)
                    setLevel(player, player.getLevel() + quantity);
                else if ( sound )
                    giveExperience(player, quantity);
                else
                	giveSoundlessExperience(player, quantity);
                break;

            case TAKE:
                if(level)
                    setLevel(player, player.getLevel() - quantity);
                else if ( sound )
                    giveExperience(player, -quantity);
                else
                	giveSoundlessExperience(player, -quantity);
                break;
        }

    }
}
