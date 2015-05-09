package io.sirfrancis.bacon.db.enums;

/**
 * Created by adam on 2/16/15.
 */
public class Indexes {
	public static final String MOVIE_IMDBID = Vertices.MOVIE + "." + MovieProps.IMDBID;
	public static final String MOVIE_OMDBID = Vertices.MOVIE + "." + MovieProps.OMDBID;
	public static final String MOVIE_INDEXTITLE = Vertices.MOVIE + "." + MovieProps.INDEXTITLE;
	public static final String MOVIE_QUIZORDER = Vertices.MOVIE + "." + MovieProps.QUIZ_ORDER;
	public static final String USER_USERNAME = Vertices.USER + "." + UserProps.USERNAME;
	public static final String PERSON_NAME = Vertices.PERSON + "." + PersonProps.NAME;
}
