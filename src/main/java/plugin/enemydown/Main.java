package plugin.enemydown;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.enemydown.command.EnemyDownCommand;
import plugin.enemydown.command.EnemySpawnCommand;

public final class Main extends JavaPlugin {

  @Override
  public void onEnable() {
    EnemyDownCommand enemyDownCommand = new EnemyDownCommand(this);
    Bukkit.getPluginManager().registerEvents(enemyDownCommand, this);
    Objects.requireNonNull(getCommand("enemyDown")).setExecutor(enemyDownCommand);
    Objects.requireNonNull(getCommand("enemySpawn")).setExecutor(new EnemySpawnCommand());
  }

}
