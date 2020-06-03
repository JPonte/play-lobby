CREATE TABLE auth.users
(
    username varchar(20) NOT NULL,
    password varchar(255) NOT NULL,
    PRIMARY KEY (username)
);

alter table auth.users owner to ponte;

create table game.game_info
(
	id serial
		constraint game_info_pk
			primary key,
	player_count int not null,
	password varchar(32),
	status integer default 0 not null
);

alter table game.game_info owner to ponte;

create table game.game_players
(
    game_id  integer     not null
        constraint game_players_game_info_id_fk
            references game.game_info
            on delete cascade,
    username varchar(20) not null
        constraint game_players_users_username_fk
            references auth.users
            on delete set null,
    constraint game_players_pk
        primary key (game_id, username)
);

alter table game.game_players owner to ponte;
