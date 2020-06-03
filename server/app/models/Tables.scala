package models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.PostgresProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = GameInfo.schema ++ GamePlayers.schema ++ Users.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table GameInfo
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param playerCount Database column player_count SqlType(int4)
   *  @param password Database column password SqlType(varchar), Length(32,true), Default(None)
   *  @param started Database column started SqlType(bool), Default(false)
   *  @param finished Database column finished SqlType(bool), Default(false) */
  case class GameInfoRow(id: Int, playerCount: Int, password: Option[String] = None, started: Boolean = false, finished: Boolean = false)
  /** GetResult implicit for fetching GameInfoRow objects using plain SQL queries */
  implicit def GetResultGameInfoRow(implicit e0: GR[Int], e1: GR[Option[String]], e2: GR[Boolean]): GR[GameInfoRow] = GR{
    prs => import prs._
    GameInfoRow.tupled((<<[Int], <<[Int], <<?[String], <<[Boolean], <<[Boolean]))
  }
  /** Table description of table game_info. Objects of this class serve as prototypes for rows in queries. */
  class GameInfo(_tableTag: Tag) extends profile.api.Table[GameInfoRow](_tableTag, Some("game"), "game_info") {
    def * = (id, playerCount, password, started, finished) <> (GameInfoRow.tupled, GameInfoRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(playerCount), password, Rep.Some(started), Rep.Some(finished))).shaped.<>({r=>import r._; _1.map(_=> GameInfoRow.tupled((_1.get, _2.get, _3, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column player_count SqlType(int4) */
    val playerCount: Rep[Int] = column[Int]("player_count")
    /** Database column password SqlType(varchar), Length(32,true), Default(None) */
    val password: Rep[Option[String]] = column[Option[String]]("password", O.Length(32,varying=true), O.Default(None))
    /** Database column started SqlType(bool), Default(false) */
    val started: Rep[Boolean] = column[Boolean]("started", O.Default(false))
    /** Database column finished SqlType(bool), Default(false) */
    val finished: Rep[Boolean] = column[Boolean]("finished", O.Default(false))
  }
  /** Collection-like TableQuery object for table GameInfo */
  lazy val GameInfo = new TableQuery(tag => new GameInfo(tag))

  /** Entity class storing rows of table GamePlayers
   *  @param gameId Database column game_id SqlType(int4)
   *  @param username Database column username SqlType(varchar), Length(20,true) */
  case class GamePlayersRow(gameId: Int, username: String)
  /** GetResult implicit for fetching GamePlayersRow objects using plain SQL queries */
  implicit def GetResultGamePlayersRow(implicit e0: GR[Int], e1: GR[String]): GR[GamePlayersRow] = GR{
    prs => import prs._
    GamePlayersRow.tupled((<<[Int], <<[String]))
  }
  /** Table description of table game_players. Objects of this class serve as prototypes for rows in queries. */
  class GamePlayers(_tableTag: Tag) extends profile.api.Table[GamePlayersRow](_tableTag, Some("game"), "game_players") {
    def * = (gameId, username) <> (GamePlayersRow.tupled, GamePlayersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(gameId), Rep.Some(username))).shaped.<>({r=>import r._; _1.map(_=> GamePlayersRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column game_id SqlType(int4) */
    val gameId: Rep[Int] = column[Int]("game_id")
    /** Database column username SqlType(varchar), Length(20,true) */
    val username: Rep[String] = column[String]("username", O.Length(20,varying=true))

    /** Primary key of GamePlayers (database name game_players_pk) */
    val pk = primaryKey("game_players_pk", (gameId, username))

    /** Foreign key referencing GameInfo (database name game_players_game_info_id_fk) */
    lazy val gameInfoFk = foreignKey("game_players_game_info_id_fk", gameId, GameInfo)(r => r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.Cascade)
    /** Foreign key referencing Users (database name game_players_users_username_fk) */
    lazy val usersFk = foreignKey("game_players_users_username_fk", username, Users)(r => r.username, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.SetNull)
  }
  /** Collection-like TableQuery object for table GamePlayers */
  lazy val GamePlayers = new TableQuery(tag => new GamePlayers(tag))

  /** Entity class storing rows of table Users
   *  @param username Database column username SqlType(varchar), PrimaryKey, Length(20,true)
   *  @param password Database column password SqlType(varchar), Length(255,true) */
  case class UsersRow(username: String, password: String)
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[String]): GR[UsersRow] = GR{
    prs => import prs._
    UsersRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends profile.api.Table[UsersRow](_tableTag, Some("auth"), "users") {
    def * = (username, password) <> (UsersRow.tupled, UsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(username), Rep.Some(password))).shaped.<>({r=>import r._; _1.map(_=> UsersRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column username SqlType(varchar), PrimaryKey, Length(20,true) */
    val username: Rep[String] = column[String]("username", O.PrimaryKey, O.Length(20,varying=true))
    /** Database column password SqlType(varchar), Length(255,true) */
    val password: Rep[String] = column[String]("password", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}
