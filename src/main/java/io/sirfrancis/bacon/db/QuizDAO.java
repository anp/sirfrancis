package io.sirfrancis.bacon.db;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import io.sirfrancis.bacon.core.Movie;

import java.util.LinkedList;
import java.util.List;

public class QuizDAO {
	OrientGraphFactory factory;
	MovieDAO movieDAO;

	public QuizDAO (OrientGraphFactory factory) {
		this.factory = factory;
		movieDAO = new MovieDAO(factory);
	}

	public List<Movie> getQuizMovies(int perPage, int pageNumber) {
		List<Movie> quizItems = new LinkedList<>();
		OrientGraph graph = factory.getTx();
		try {
			Vertex quizVertex = graph.getVertexByKey("quizStart.identifier", "quiz starting point");
			for (int i = 0; i < perPage * pageNumber; i++) {
				Vertex nextVertex = null;
				for (Vertex next : quizVertex.getVertices(Direction.BOTH, "Movie")) {
					nextVertex = next;
				}
				if (nextVertex != null) {
					quizItems.add(movieDAO.buildMovie(nextVertex));
				} else {
					break;
				}
			}
		} finally {
			graph.shutdown();
		}
		if (pageNumber > 1) {
			int fromIndex = (perPage * pageNumber) - 1;
			quizItems = quizItems.subList(fromIndex, quizItems.size()-1);
		}
		return quizItems;
	}
}
