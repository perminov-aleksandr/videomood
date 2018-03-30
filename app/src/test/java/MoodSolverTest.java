import org.junit.Test;

import ru.spbstu.videomood.Mood;
import ru.spbstu.videomood.MuseMoodSolver;
import ru.spbstu.videomood.User;

import static org.junit.Assert.assertTrue;

public class MoodSolverTest {

    private MuseMoodSolver moodSolver = new MuseMoodSolver(this);

    @Test
    public void setGetUserMood() {
        User.setCurrentMood(Mood.AWFUL);

        assertTrue(User.getCurrentMood() == Mood.AWFUL);
    }

    @Test
    public void solveMood() {
        User.setCurrentMood(Mood.NORMAL);
        double[] scores = new double[]{
            50, 55, 20, 190, 75
        };

        moodSolver.solve(scores);
        Mood changedMood = User.getCurrentMood();
        assertTrue(changedMood == Mood.AWFUL);
    }
}
