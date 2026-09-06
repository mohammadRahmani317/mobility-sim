package ir.srbiau.cloudsim.mobilitysim.workload;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.UserLevel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Component
public class WorkloadGenerator {

    public static final long DEFAULT_RANDOM_SEED = 2023L;
    public static final double DEADLINE_FACTOR = 2.5;
    public static final double DEADLINE_REFERENCE_MIPS = 1000.0;
    private static final int EASY_USER_COUNT = 10;//10
    private static final int MEDIUM_USER_COUNT = 6;//6
    private static final int HARD_USER_COUNT = 4;//4
    private final long randomSeed;

    public WorkloadGenerator() {
        this(DEFAULT_RANDOM_SEED);
    }

    public WorkloadGenerator(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public List<User> generateUsers() {
        List<User> users = new ArrayList<>();

        Random random = new Random(randomSeed);
        int[] nextUserId = {0};
        int[] nextCloudletId = {0};

        generateUserLevelUsers(users, UserLevel.EASY, EASY_USER_COUNT, random, nextUserId, nextCloudletId);
        generateUserLevelUsers(users, UserLevel.MEDIUM, MEDIUM_USER_COUNT, random, nextUserId, nextCloudletId);
        generateUserLevelUsers(users, UserLevel.HARD, HARD_USER_COUNT, random, nextUserId, nextCloudletId);

        return users;
    }

    private void generateUserLevelUsers(List<User> users, UserLevel userLevel, int userCount, Random random,
                                        int[] nextUserId, int[] nextCloudletId) {
        for (int i = 0; i < userCount; i++) {
            int userId = nextUserId[0]++;
            User user = new User(userId, userLevel, userLevel.name() + "-" + userId);
            generateCloudlets(user, userLevel, random, nextCloudletId);
            users.add(user);
        }
    }

    private void generateCloudlets(User user, UserLevel userLevel, Random random, int[] nextCloudletId) {
        int numCloudlets = getCloudletCount(userLevel, random);
        int lengthMin = getCloudletMinLength(userLevel);
        int lengthMax = getCloudletMaxLength(userLevel);


        for (int i = 0; i < numCloudlets; i++) {
            long length = random.nextLong(lengthMin, lengthMax);
            CloudletMobility cloudlet = createCloudlet(nextCloudletId[0]++, length, user);
            user.addCloudlet(cloudlet);
        }

        assignPriority(user.getCloudlets());
        createDag(user, random);

        // The generator has no VM context, so 1000 MIPS is an explicit reference capacity.
        // SDMS later evaluates the resulting deadline against the actual candidate VMs.
        double estimatedSerialTime = user.getCloudlets().stream()
                .mapToDouble(cloudlet -> cloudlet.getCloudletLength() / DEADLINE_REFERENCE_MIPS)
                .sum();
        user.setDeadline(estimatedSerialTime * DEADLINE_FACTOR);
    }

    private int getCloudletCount(UserLevel userLevel, Random random) {
        return switch (userLevel) {
            case EASY -> random.nextInt(3) + 2;
            case MEDIUM -> random.nextInt(4) + 5;
            case HARD -> random.nextInt(8) + 8;
        };
    }

    private int getCloudletMinLength(UserLevel userLevel) {
        return switch (userLevel) {
            case EASY -> 1000;
            case MEDIUM -> 5000;
            case HARD -> 20000;
        };
    }

    private int getCloudletMaxLength(UserLevel userLevel) {
        return switch (userLevel) {
            case EASY -> 5000;
            case MEDIUM -> 20000;
            case HARD -> 50000;
        };
    }

    private CloudletMobility createCloudlet(int id, long length, User user) {
        UtilizationModelFull utilizationModel = new UtilizationModelFull();

        return new CloudletMobility(
                id, length, 1, 300L, 400L,
                utilizationModel, utilizationModel, utilizationModel,user
        );
    }

    private void assignPriority(List<CloudletMobility> cloudlets) {
        List<CloudletMobility> sorted = new ArrayList<>(cloudlets);
        sorted.sort(Comparator.comparingLong(CloudletMobility::getCloudletLength).reversed());

        int priority = 1;
        for (CloudletMobility cloudlet : sorted) {
            cloudlet.setPriority(priority++); // Assign priority based on length
        }
    }

    private void createDag(User user, Random random) {
        List<CloudletMobility> cloudlets = user.getCloudlets();

        // Forward-only edges guarantee acyclicity. Consecutive edges keep every task
        // connected, while skip edges create understandable forks/joins.
        if (cloudlets.size() >= 2) {
            addEdge(user, cloudlets.get(0), cloudlets.get(1), random);
        }
        if (cloudlets.size() >= 3) {
            addEdge(user, cloudlets.get(0), cloudlets.get(2), random);
        }
        for (int i = 3; i < cloudlets.size(); i++) {
            addEdge(user, cloudlets.get(i - 1), cloudlets.get(i), random);
            if (i % 2 == 1) {
                addEdge(user, cloudlets.get(i - 2), cloudlets.get(i), random);
            }
        }
    }

    private void addEdge(User user, CloudletMobility source, CloudletMobility target, Random random) {
        double dataSizeMb = 10.0 + random.nextInt(91);
        user.addWorkflowEdge(source, target, dataSizeMb);
    }
}
