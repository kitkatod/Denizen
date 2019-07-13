package net.aufdemrand.denizen.scripts.commands.npc;

import net.aufdemrand.denizen.npc.traits.AssignmentTrait;
import net.aufdemrand.denizen.utilities.Utilities;
import net.aufdemrand.denizen.utilities.debugging.dB;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.aH;
import com.denizenscript.denizencore.objects.dScript;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class AssignmentCommand extends AbstractCommand {

    // <--[command]
    // @Name Assignment
    // @Syntax assignment [set/remove] (script:<name>)
    // @Required 1
    // @Plugin Citizens
    // @Short Changes an NPC's assignment.
    // @Group npc
    //
    // @Description
    // Changes an NPC's assignment as though you used the '/npc assignment' command.
    // Uses the script: argument, which accepts an assignment script type. For this command to work an npc must
    // be attached to the script queue or an npc specified with npc:n@npc.
    //
    // @Tags
    // <n@npc.script>
    // <n@npc.has_script>
    // <server.list_npcs_assigned[<assignment_script>]>
    //
    // @Usage
    // Use to assign an npc with an assignment script named 'Bob the Builder'.
    // - assignment set "script:Bob the Builder"
    //
    // @Usage
    // Use to give an npc with the id of 3 an assignment.
    // - assignment set "script:Bob the Builder" npc:n@3
    //
    // @Usage
    // Use to remove an npc's assignment.
    // - assignment remove
    // -->

    private enum Action {SET, REMOVE}

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        // Parse Arguments
        for (aH.Argument arg : aH.interpretArguments(scriptEntry.aHArgs)) {

            if (arg.matchesEnum(Action.values())
                    && !scriptEntry.hasObject("action")) {
                scriptEntry.addObject("action", Action.valueOf(arg.getValue().toUpperCase()));
            }
            else if (arg.matchesArgumentType(dScript.class)
                    && !scriptEntry.hasObject("script")) {
                // Check the type of script.. it must be an assignment-type container
                if (arg.asType(dScript.class) != null
                        && arg.asType(dScript.class).getType().equalsIgnoreCase("assignment")) {
                    scriptEntry.addObject("script", arg.asType(dScript.class));
                }
                else {
                    throw new InvalidArgumentsException("Script specified is not an 'assignment-type' container.");
                }
            }
            else {
                arg.reportUnhandled();
            }
        }

        // Check required arguments
        if (!Utilities.entryHasNPC(scriptEntry)) {
            throw new InvalidArgumentsException("NPC linked was missing or invalid.");
        }

        if (!scriptEntry.hasObject("action")) {
            throw new InvalidArgumentsException("Must specify an action!");
        }

        if (scriptEntry.getObject("action").equals(Action.SET) && !scriptEntry.hasObject("script")) {
            throw new InvalidArgumentsException("Script specified was missing or invalid.");
        }

    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        dScript script = scriptEntry.getdObject("script");

        // Report to dB
        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), aH.debugObj("action", scriptEntry.getObject("action")) + (script != null ? script.debug() : ""));
        }

        // Perform desired action
        if (scriptEntry.getObject("action").equals(Action.SET)) {
            Utilities.getEntryNPC(scriptEntry).getCitizen().getTrait(AssignmentTrait.class)
                    .setAssignment(script.getName(), Utilities.getEntryPlayer(scriptEntry));
        }
        else if (scriptEntry.getObject("action").equals(Action.REMOVE)) {
            Utilities.getEntryNPC(scriptEntry).getCitizen().getTrait(AssignmentTrait.class)
                    .removeAssignment(Utilities.getEntryPlayer(scriptEntry));
        }
    }
}
