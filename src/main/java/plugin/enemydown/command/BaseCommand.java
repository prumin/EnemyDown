package plugin.enemydown.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * コマンドを実行して動かすプラグイン処理の基底クラスです。
 */
public abstract class BaseCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
    if (commandSender instanceof Player player) {
      return onExecutePlayerCommand(player, command, s, strings);
    } else {
      return onExecuteNPCCommand(commandSender, command, s, strings);
    }
  }

  /**
   *　コマンド実行者がプレイヤーだった場合に実行します。
   *
   * @param player　コマンドを実行したプレイヤー
   * @param command コマンド
   * @param  s ラベル
   * @param  strings　コマンド引数
   * @return　処理の実行有無
   */
  public abstract boolean onExecutePlayerCommand(Player player, Command command, String s, String[] strings);

  /**
   * コマンド実行者がプレイヤー以外だった場合に実行します。
   *
   * @param commandSender　コマンド実行者
   * @param command コマンド
   * @param  s ラベル
   * @param  strings　コマンド引数
   * @return　処理の実行有無
   */
  public abstract boolean onExecuteNPCCommand(CommandSender commandSender, Command command, String s, String[] strings);
}
