package ir.srbiau.cloudsim.mobilitysim.workload;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.UserLevel;
import org.cloudbus.cloudsim.UtilizationModelStochastic;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Component
public class WorkloadGenerator {

    private static final Random RANDOM = new Random();
    private static final int EASY_USER_COUNT = 10;//10
    private static final int MEDIUM_USER_COUNT = 6;//6
    private static final int HARD_USER_COUNT = 4;//4

    public List<User> generateUsers() {
        List<User> users = new ArrayList<>();

        generateUserLevelUsers(users, UserLevel.EASY, EASY_USER_COUNT);
        generateUserLevelUsers(users, UserLevel.MEDIUM, MEDIUM_USER_COUNT);
        generateUserLevelUsers(users, UserLevel.HARD, HARD_USER_COUNT);

        return users;
    }

    private void generateUserLevelUsers(List<User> users, UserLevel userLevel, int userCount) {
        for (int i = 0; i < userCount; i++) {
            User user = new User(i, userLevel,userLevel.name());
            generateCloudlets(user, userLevel);
            users.add(user);
        }
    }

    private void generateCloudlets(User user, UserLevel userLevel) {
        int numCloudlets = getCloudletCount(userLevel);
        int lengthMin = getCloudletMinLength(userLevel);
        int lengthMax = getCloudletMaxLength(userLevel);


        for (int i = 0; i < numCloudlets; i++) {
            long length = RANDOM.nextLong(lengthMin, lengthMax);
            CloudletMobility cloudlet = createCloudlet(i, length, user);
            user.addCloudlet(cloudlet);
        }

        assignPriority(user.getCloudlets());
    }

    private int getCloudletCount(UserLevel userLevel) {
        return switch (userLevel) {
            case EASY -> RANDOM.nextInt(3) + 2;
            case MEDIUM -> RANDOM.nextInt(4) + 5;
            case HARD -> RANDOM.nextInt(8) + 8;
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
        UtilizationModelStochastic utilizationModel = new UtilizationModelStochastic();

        return new CloudletMobility(
                id, length, 1, 300L, 400L,
                utilizationModel, utilizationModel, utilizationModel,user
        );
    }

    private void assignPriority(List<CloudletMobility> cloudlets) {
        cloudlets.sort(Comparator.comparingLong(CloudletMobility::getCloudletLength).reversed());

        int priority = 1;
        for (CloudletMobility cloudlet : cloudlets) {
            cloudlet.setPriority(priority++); // Assign priority based on length
        }
    }

    // Optional DAG creation method (commented out in your original code)
    private void createDag(User user) {
        List<CloudletMobility> cloudlets = user.getCloudlets();
        cloudlets.sort(Comparator.comparingInt(CloudletMobility::getPriority).reversed());

        for (int i = 0; i < cloudlets.size(); i++) {
            CloudletMobility cloudlet = cloudlets.get(i);
            List<CloudletMobility> parents = new ArrayList<>();

            for (int j = 0; j < i; j++) {
                CloudletMobility parent = cloudlets.get(j);
                if (cloudlet.getPriority() > parent.getPriority()) {
                    parents.add(parent);
                }
            }

            cloudlet.setParents(parents);
        }

        System.out.println("DAG created for user " + user.getId() + " with " + cloudlets.size() + " cloudlets.");
    }
}