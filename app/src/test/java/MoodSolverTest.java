import android.util.Range;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.spbstu.videomood.Mood;
import ru.spbstu.videomood.MuseMoodSolver;
import ru.spbstu.videomood.User;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MoodSolverTest {

    @Mock
    Range<Integer> ageRange;

    private User user = new User(ageRange);

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
