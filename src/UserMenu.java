import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Scanner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMenu {
    private Connection connection;
    private Scanner sc = new Scanner(System.in);
    private String userID;
    private String type;
    private Date loginTime;
    private HashMap<String, ArrayList<Integer>> lotAndSpot = new HashMap<String, ArrayList<Integer>>();

    public UserMenu(Connection connection, String userID, Date loginTime) {
        setConnection(connection);
        setUserID(userID);
        setLoginTime(loginTime);
        getUserType();
        getLotAndSpot();
    }

    public String getUserID() {
        return userID;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    private void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }

    public boolean isNumeric(String str) {
        if (str == null || str.strip() == "") {
            return false;
        } else {
            try {
                Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public void getLotAndSpot() {
        try {
            Statement st = connection.createStatement();
            ResultSet rset = st.executeQuery("SELECT * FROM parking.spot;");

            while (rset.next()) {
                int spotId = rset.getInt(1);
                String lotId = rset.getString(2);

                if (lotAndSpot.get(lotId) == null) {
                    ArrayList<Integer> temp = new ArrayList<Integer>();
                    temp.add(spotId);
                    lotAndSpot.put(lotId, temp);
                } else {
                    ArrayList<Integer> temp = lotAndSpot.get(lotId);
                    temp.add(spotId);
                    lotAndSpot.put(lotId, temp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getUserType() {
        try {
            PreparedStatement pst = connection.prepareStatement("SELECT * FROM parking.member NATURAL JOIN parking.user WHERE parking.member.user_id = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setString(1, userID);
            ResultSet rset = pst.executeQuery();

            if (rset.next()) {
                type = "member";
            } else {
                PreparedStatement pstUser = connection.prepareStatement("SELECT * FROM parking.user WHERE parking.user.user_id = ?");
                pstUser.setString(1, userID);
                ResultSet userResult = pstUser.executeQuery();

                if (userResult.next()) {
                    type = "user";
                } else {
                    type = "employee";
                }

                userResult.close();
                pstUser.close();
            }
            rset.close();
            pst.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String UserMenuOptions() {
        System.out.println("\nUser Menu");
        System.out.println("1. View Profile\n2. Update Profile\n3. Make Reservation\n4. Logout");
        System.out.print("Enter your option here: ");
        String option = sc.nextLine();
        return option;
    }

    public void UserMenuScreen() {
        boolean exit = false;
        while (!exit) {
            String option = UserMenuOptions();

            switch (option) {
                case "1":
                    viewProfileScreen();
                    break;
                case "2":
                    updateProfileScreen();
                    break;
                case "3":
                    makeReservationScreen();
                    break;
                case "4":
                    logout();
                    exit = true;
                    break;
                default:
                    System.out.println("\nInvalid option. Try again.");
            }
        }
    }

    public void viewProfileScreen() {
        try {
            if (type == "member") {
                PreparedStatement pstMember = connection.prepareStatement("SELECT * FROM parking.user NATURAL JOIN parking.member, parking.parking_lot WHERE parking.member.user_id = ? AND parking.member.lot_id = parking.parking_lot.lot_id");
                pstMember.setString(1, userID);
                ResultSet memberResult = pstMember.executeQuery();
                
                if (memberResult.next()) {
                    printUserProfile(memberResult, type);
                }

                memberResult.close();
                pstMember.close();
            } else if (type == "user") {
                    PreparedStatement pstUser = connection.prepareStatement("SELECT * FROM parking.user WHERE parking.user.user_id = ?");
                    pstUser.setString(1, userID);
                    ResultSet userResult = pstUser.executeQuery();
                    
                    if (userResult.next()) {
                        printUserProfile(userResult, type);
                    }

                    userResult.close();
                    pstUser.close();
                } else if (type == "employee") {
                    PreparedStatement pstEmployee = connection.prepareStatement("SELECT * FROM parking.employee WHERE parking.employee.employee_id = ?");
                    pstEmployee.setString(1, userID);
                    ResultSet employeeResult = pstEmployee.executeQuery();

                    if (employeeResult.next()) {
                        printUserProfile(employeeResult, type);
                    }

                    pstEmployee.close();
                    employeeResult.close();
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printUserProfile(ResultSet rset, String type) {
        try {
            System.out.println("\nUser Profile");
            System.out.println(String.format("Name: %s", rset.getString("name")));
            System.out.println(String.format("Password: %s", rset.getString("password")));

            if (type == "member") {
                System.out.println(String.format("Registered license plate: %s", rset.getString("registered_license_plate")));
                System.out.println(String.format("Registered lot: %s", rset.getString("lot_id")));
                System.out.println(String.format("Registered spot: %d", rset.getInt("spot_id")));
                System.out.println(String.format("Membership fee: %f", rset.getDouble("membership_fee")));
            } else if (type == "employee") {
                System.out.println(String.format("Type: %s", rset.getString("type")));
                System.out.println(String.format("Salary: %f", rset.getDouble("salary")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String updateProfileOptions() {
        System.out.println("\nUpdate Profile");
        System.out.println("1. Name\n2. Password");

        if (type == "member") {
            System.out.println("3. Lot\n4. Spot\n5. Exit");
        }

        System.out.print("Enter option here: ");
        String option = sc.nextLine();
        return option;
    }

    public String getNewValue() {
        System.out.print("Enter new value here: ");
        String newValue = sc.nextLine();
        return newValue;
    }

    public void updateProfileScreen() {
        boolean exit = false;

        while (!exit) {
            String option = updateProfileOptions();
            boolean isValid = false;

            switch (option) {
                case "1":
                    while (!isValid) {
                        String value = getNewValue();
                        if (value.length() <= 30) {
                            makeProfileUpdate("name", value);
                            isValid = true;
                        } else {
                            System.out.println("\nInvalid input. Try again.");
                        }
                    }
                    break;
                case "2":
                    while (!isValid) {
                        String value = getNewValue();
                        if (value.length() <= 20) {
                            makeProfileUpdate("password", value);
                            isValid = true;
                        } else {
                            System.out.println("\nInvalid input. Try again.");
                        }
                    }
                    break;
                case "3":
                    while (!isValid) {
                        String value = getNewValue();
                        if (value.length() == 1 && lotAndSpot.keySet().contains(value)) {
                            makeProfileUpdate("lot_id", value);
                            isValid = true;
                        } else {
                            System.out.println("\nInvalid input. Try again.");
                        }
                    }
                    break;
                case "4":
                    if (type == "member") {
                        while (!isValid) {
                            String value = getNewValue();
                            String lotId = "";
                            try {
                                PreparedStatement pst = connection.prepareStatement("SELECT * FROM parking.member WHERE parking.member.user_id = ?");
                                pst.setString(1, userID);
                                ResultSet rset = pst.executeQuery();

                                if (rset.next()) {
                                    lotId = rset.getString("lot_id");
                                }
                                
                                rset.close();
                                pst.close();

                                if (isNumeric(value)) {
                                    if (lotAndSpot.get(lotId).contains(Integer.parseInt(value))) {
                                        makeProfileUpdate("spot_id", value);
                                        isValid = true;
                                    } else {
                                        System.out.println("\nInvalid input. Try again.");
                                    }
                                } else {
                                    System.out.println("\nInvalid input. Try again.");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.out.println("\nInvalid option. Try again.");
                    }
                    break;
                case "5":
                    exit = true;
                    break;
                default:
                    System.out.println("\nInvalid option. Try again.");
            }
        }
    }
    
    public void makeProfileUpdate(String updateField, String newValue) {
        if (updateField == "name" || updateField == "password") {
            try {
                PreparedStatement pst = connection.prepareStatement(String.format("UPDATE parking.user SET %s = ? WHERE user_id = ?", updateField));
                pst.setString(1, newValue);
                pst.setString(2, userID);
                pst.executeUpdate();

                System.out.println("Update profile successfully.");

                pst.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                PreparedStatement pst = connection.prepareStatement(String.format("UPDATE parking.member SET %s = ? WHERE user_id = ?", updateField));
                if (updateField == "spot_id") {
                    pst.setInt(1, Integer.parseInt(newValue));
                } else {
                    pst.setString(1, newValue);
                }
                pst.setString(2, userID);
                pst.executeUpdate();

                System.out.println("Update profile successfully.");

                pst.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
            _________________________________________
        /  TODO: Finish this shitty code           \
        \                                         /
        -----------------------------------------
                \    ^__^
                \   (oo)\_______
                    (__)\       )\/\
                        ||----w |
                       ||     ||
    */
    public void makeReservationScreen() {}

    public void logout() {
        try {
            PreparedStatement pst = connection.prepareStatement("UPDATE parking.user SET login_time = ?, logout_time = ? WHERE user_id = ?");
            pst.setTimestamp(1, new Timestamp(loginTime.getTime()));
            pst.setTimestamp(2, new Timestamp(new Date().getTime()));
            pst.setString(3, userID);
            pst.executeUpdate();

            System.out.println("\nLogging out...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
