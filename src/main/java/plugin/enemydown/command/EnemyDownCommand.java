package plugin.enemydown.command;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import plugin.enemydown.Main;
import plugin.enemydown.data.PlayerScore;

/**
 * 制限時間内にランダムで出現する敵を倒して、スコアを獲得するゲームを起動するコマンドです。 スコアは敵によって変わり、倒せた敵の合計によってスコアが変動します。 結果はプレイヤー名、点数、日時などで保存されます。
 */
public class EnemyDownCommand extends BaseCommand implements Listener {

  public static final int GAME_TIME = 20;
  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";
  public static final String NONE = "none";
  public static final String LIST = "List";

  private Main main;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private List<Entity> spawnEntityList = new ArrayList<>();


  public EnemyDownCommand(Main main) {
    this.main = main;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String s, String[] strings) {
    player.sendMessage("Command received: " + s + ", args: " + String.join(", ", strings));

    if (strings.length == 1 && LIST.equals(strings[0])) {
      player.sendMessage("List command recognized");

      try (Connection con = DriverManager.getConnection(
          "jdbc:mysql://localhost:3306/spigot_server",
          "root",
          "L4GgbiBhzRjB");
          Statement statement = con.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT * FROM player_score;")) {

        player.sendMessage("Database query executed");

        while (resultSet.next()) {
          int id = resultSet.getInt("id");
          String name = resultSet.getString("player_name");
          int score = resultSet.getInt("score");
          String difficulty = resultSet.getString("difficulty");

          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
          LocalDateTime data = LocalDateTime.parse(resultSet.getString("registered_at"), formatter);

          player.sendMessage(id + " | " + name + " | " + score + " | " + difficulty + " | " + data.format(formatter));
        }
      } catch (SQLException e) {
        e.printStackTrace();
        player.sendMessage(ChatColor.RED + "An error occurred while accessing the database: " + e.getMessage());
      }
      return false;
    }
    String difficulty = getDifficulty(player, strings);
    if (difficulty.equals(NONE)) {
      return false;
    }

    PlayerScore nowPlayerScore = getPlayerScore(player);

    initPlayerStatus(player);

    gamePlay(player, nowPlayerScore, difficulty);
    return true;
  }

  /**
   * 難易度をコマンド引数から取得します。
   *
   * @param player  　コマンドを実行したプレイヤー
   * @param strings 　コマンド引数
   * @return　難易度
   */
  private String getDifficulty(Player player, String[] strings) {
    if (strings.length == 1 && (EASY.equals(strings[0]) || NORMAL.equals(strings[0]) || HARD.equals(strings[0]))) {
      return strings[0];
    }
    player.sendMessage(ChatColor.DARK_RED + "実行できません。コマンド引数の1つ目に難易度指定が必要です。[easy, normal, hard]");
    return NONE;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender commandSender, Command command, String s, String[] strings) {
    return false;
  }

  @EventHandler
  public void onEnemyDeath(EntityDeathEvent e) {
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();

    if (Objects.isNull(player) || spawnEntityList.stream().noneMatch(entity -> entity.equals(enemy))) {
      return;
    }

    playerScoreList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {
          int point = switch (enemy.getType()) {
            case ZOMBIE, SPIDER -> 10;
            case SKELETON -> 20;
            case WITCH -> 30;
            case IRON_GOLEM -> 50;
            case CHICKEN -> 100;
            default -> 0;
          };

          p.setScore(p.getScore() + point);
          player.sendMessage("敵を倒した！ 現在のスコアは" + p.getScore() + "点！");
        });

  }

  /**
   * 現在実行しているプレイヤーのスコア情報を取得する。
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return　現在実行しているプレイヤーのスコア情報
   */
  private PlayerScore getPlayerScore(Player player) {
    PlayerScore playerScore = new PlayerScore(player.getName());

    if (playerScoreList.isEmpty()) {
      playerScore = addNewPlayer(player);
    } else {
      playerScore = playerScoreList.stream()
          .findFirst()
          .map(ps -> ps.getPlayerName().equals(player.getName())
              ? ps
              : addNewPlayer(player)).orElse(playerScore);
    }

    playerScore.setGameTime(GAME_TIME);
    playerScore.setScore(0);
    removePotionEffect(player);
    return playerScore;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return 新規プレイヤー
   */
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore(player.getName());
    playerScoreList.add(newPlayer);
    return newPlayer;
  }

  /**
   * ゲームを始める前にプレイヤーの状態を設定する。 体力と空腹度を最大にして、装備は鉄一式になる。
   *
   * @param player 　コマンドを実行したプレイヤー
   */
  private void initPlayerStatus(Player player) {
    player.setHealth(20);
    player.setFoodLevel(20);

    PlayerInventory inventory = player.getInventory();
    inventory.setHelmet(new ItemStack(Material.IRON_HELMET));
    inventory.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
    inventory.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
    inventory.setBoots(new ItemStack(Material.IRON_BOOTS));
    inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
  }

  /**
   * ゲームを実行します。規定の時間内に敵を倒すとスコアが加算されます。合計スコアを時間経過後に表示します。
   *
   * @param player         　コマンドを実行したプレイヤー
   * @param nowPlayerScore 　プレイヤースコア情報
   * @param difficulty     難易度
   */
  private void gamePlay(Player player, PlayerScore nowPlayerScore, String difficulty) {
    Bukkit.getScheduler().runTaskTimer(main, Runnable -> {
      if (nowPlayerScore.getGameTime() <= 0) {
        Runnable.cancel();

        player.sendTitle("ゲームが終了しました。",
            nowPlayerScore.getPlayerName() + "合計" + nowPlayerScore.getScore() + "点!",
            0, 60, 0);

        spawnEntityList.forEach(Entity::remove);
        spawnEntityList.clear();

        removePotionEffect(player);
        return;
      }
      Entity spawnEntity = player.getWorld().spawnEntity(getEnemySpawnLocation(player), getEnemy(difficulty));
      spawnEntityList.add(spawnEntity);
      nowPlayerScore.setGameTime(nowPlayerScore.getGameTime() - 5);
    }, 0, 5 * 20);
  }

  /**
   * 敵の出現エリアを取得します。 出現エリアはX軸とZ軸は自分の位置からプラス、ランダムで－10～9の値が設定されます。 Y軸はプレイヤーと同じ位置になります。
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return　敵の出現場所
   */
  private Location getEnemySpawnLocation(Player player) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(20) - 10;
    int randomZ = new SplittableRandom().nextInt(20) - 10;

    double x = playerLocation.getX() + randomX;
    double y = playerLocation.getY();
    double z = playerLocation.getZ() + randomZ;

    return new Location(player.getWorld(), x, y, z);
  }

  /**
   * ランダムで敵を抽選して、その結果の敵を取得します。
   *
   * @param difficulty 難易度
   * @return　敵
   */
  private EntityType getEnemy(String difficulty) {
    List<EntityType> enemyList = switch (difficulty) {
      case NORMAL -> List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.WITCH);
      case HARD -> List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.WITCH, EntityType.IRON_GOLEM);
      default -> List.of(EntityType.ZOMBIE, EntityType.SPIDER);
    };
    return enemyList.get(new SplittableRandom().nextInt(enemyList.size()));
  }

  /**
   * プレイヤーに設定されている特殊効果を除外します。
   *
   * @param player 　コマンドを実行したプレイヤー
   */
  private void removePotionEffect(Player player) {
    player.getActivePotionEffects().stream()
        .map(PotionEffect::getType)
        .forEach(player::removePotionEffect);
  }
}
