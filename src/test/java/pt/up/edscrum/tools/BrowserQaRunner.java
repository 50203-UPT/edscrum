package pt.up.edscrum.tools;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class BrowserQaRunner {
    public static void main(String[] args) throws Exception {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        try {
            System.out.println("Browser QA: starting headless Chrome");
            driver.get("http://localhost:8080/");
            // wait a moment for scripts to initialize
            Thread.sleep(800);

            // Login as PO (f@upt.pt) using the index page's JS-driven form
            WebElement email = driver.findElement(By.id("teacher-email"));
            WebElement password = driver.findElement(By.id("teacher-password"));
            email.clear();
            email.sendKeys("f@upt.pt");
            password.clear();
            password.sendKeys("33Vi1ItG");
            WebElement loginBtn = driver.findElement(By.cssSelector("button[onclick*='login']"));
            loginBtn.click();

            Thread.sleep(1200);

            // Navigate to homepage
            driver.get("http://localhost:8080/");
            Thread.sleep(500);

            // Try opening complete confirm modal
            try {
                WebElement completeBtn = driver.findElement(By.xpath("//button[contains(text(),'Concluir Projeto') or contains(text(),'Concluir')]"));
                completeBtn.click();
                Thread.sleep(500);
                boolean modalPresent = driver.findElements(By.id("completeProjectModal")).size() > 0;
                System.out.println("Complete confirm modal present: " + modalPresent);
            } catch (Exception e) {
                System.out.println("Complete button not found or interaction failed: " + e.getMessage());
            }

            System.out.println("Browser QA: finished");
        } finally {
            driver.quit();
        }
    }
}
