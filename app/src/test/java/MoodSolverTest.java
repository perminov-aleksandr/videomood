import org.junit.Test;

import ru.spbstu.videomood.Mood;
import ru.spbstu.videomood.MuseMoodSolver;
import ru.spbstu.videomood.User;
import ru.spbstu.videomood.Range;

import static org.junit.Assert.assertTrue;

public class MoodSolverTest {

    private User user = new User(new Range<>(0, 6));

    private MuseMoodSolver moodSolver = new MuseMoodSolver(user);

    @Test
    public void setGetUserMood() {
        user.setCurrentMood(Mood.AWFUL);

        assertTrue(user.getCurrentMood() == Mood.AWFUL);
    }

    @Test
    public void solveMood() {
        user.setCurrentMood(Mood.NORMAL);
        double[] scores = new double[]{
            50, 55, 20, 190, 75
        };

        moodSolver.solve(scores);
        Mood changedMood = user.getCurrentMood();
        assertTrue(changedMood == Mood.AWFUL);
    }
}
