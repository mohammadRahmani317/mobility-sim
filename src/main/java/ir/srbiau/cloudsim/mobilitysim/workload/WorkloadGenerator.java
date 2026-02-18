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
    private static final Random random = new Random();

    public List<User> generateUsers() {
        List<User> users = new ArrayList<>();
        int easyUsers = 10, mediumUsers = 6, hardUsers = 4;

        for (int i = 0; i < easyUsers; i++) {
            User user = new User(i);
            generateCloudlets(user, UserLevel.EASY);
            users.add(user);
        }

        for (int i = easyUsers; i < easyUsers + mediumUsers; i++) {
            User user = new User(i);
            generateCloudlets(user, UserLevel.MEDIUM);
            users.add(user);
        }

        for (int i = easyUsers + mediumUsers; i < easyUsers + mediumUsers + hardUsers; i++) {
            User user = new User(i);
            generateCloudlets(user, UserLevel.HARD);
            users.add(user);
        }

        return users;
    }

    private void generateCloudlets(User user, UserLevel userLevel) {
        int numCloudlets = 0;
        int lengthMin = 1000, lengthMax = 5000;

        switch (userLevel) {
            case UserLevel.EASY:
                numCloudlets = random.nextInt(3) + 2; // 2-4 Cloudlet
                break;
            case UserLevel.MEDIUM:
                numCloudlets = random.nextInt(4) + 5; // 5-8 Cloudlet
                lengthMin = 5000;
                lengthMax = 20000;
                break;
            case UserLevel.HARD:
                numCloudlets = random.nextInt(8) + 8; // 8-15 Cloudlet
                lengthMin = 20000;
                lengthMax = 50000;
                break;
        }

        for (int i = 0; i < numCloudlets; i++) {
            long length = random.nextLong(lengthMin, lengthMax);

            UtilizationModelStochastic utilizationModelStochastic = new UtilizationModelStochastic();

            CloudletMobility cloudlet = new CloudletMobility(
                    i,
                    length,
                    1,
                    300L,
                    400L,
                    utilizationModelStochastic,
                    utilizationModelStochastic,
                    utilizationModelStochastic,
                    user.getId()
            );

            user.addCloudlet(cloudlet);
        }
        assignPriority(user.getCloudlets());
        createDag(user);
    }

    private void assignPriority(List<CloudletMobility> cloudlets) {
        cloudlets.sort(Comparator.comparingLong(CloudletMobility::getCloudletLength).reversed());

        int priority = 1;
        for (CloudletMobility cloudlet : cloudlets) {
            cloudlet.setPriority(priority++);  // تخصیص اولویت به ترتیب
        }
    }

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


