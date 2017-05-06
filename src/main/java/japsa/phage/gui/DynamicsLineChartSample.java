package japsa.phage.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DynamicsLineChartSample extends Application {

    @Override
    public void start(Stage primaryStage) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart barChart = new BarChart(xAxis, yAxis);
        barChart.setData(getChartData());
        barChart.setTitle("A");
        primaryStage.setTitle("BarChart example");

        StackPane root = new StackPane();
        root.getChildren().add(barChart);
        primaryStage.setScene(new Scene(root, 400, 250));
        primaryStage.show();
    }

    private ObservableList<XYChart.Series<String, Double>> getChartData() {
        double aValue = 17.56;
        double cValue = 17.06;
        ObservableList<XYChart.Series<String, Double>> answer = FXCollections.observableArrayList();
        XYChart.Series<String, Double> aSeries = new XYChart.Series<String, Double>();
        XYChart.Series<String, Double> cSeries = new XYChart.Series<String, Double>();
        aSeries.setName("a");
        cSeries.setName("C");

        for (int i = 2011; i < 2021; i++) {
            aSeries.getData().add(new XYChart.Data(Integer.toString(i), aValue));
            aValue = aValue + Math.random() - .5;
            cSeries.getData().add(new XYChart.Data(Integer.toString(i), cValue));
            cValue = cValue + Math.random() - .5;
        }
        answer.addAll(aSeries, cSeries);
        return answer;
    }

    public static void main(String[] args) {
        launch(args);
    }
}