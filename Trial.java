/*
 * YEAR 1 first GIS software...under more development
 *It can do coordinate transformation, open and query data from shapefile, reading csv file coordinates into shapefile,     
 */

package org.geotools.tutorial.crs;

import java.awt.BorderLayout;
 import java.awt.Dimension;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.swing.data.JDataStoreWizard;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.geotools.swing.wizard.JWizard;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;


import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.action.SafeAction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;


import javax.swing.UIManager;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import org.geotools.data.FeatureWriter;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.tutorial.crs.CRSLab.ExportShapefileAction;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


/**
 
 */
@SuppressWarnings("serial")

public class Trial extends JFrame {
  
    //declaring variables to hold the different data values
    private DataStore dataStore;
    private final JComboBox<String> featureTypeCBox;
    private final JTable table;
    private  final JTextField text;
     private File sourceFile;
    private SimpleFeatureSource featureSource;
    private MapContext map;
  
    
    
    //this method gets the new created shapefile from the csv file
    private static File getNewShapeFile(File csvFile) {
        String path = csvFile.getAbsolutePath();
        String newPath = path.substring(0, path.length() - 4) + ".shp";

        //prompts the user to choose the file name and directory to store the newly created shapefile...
        JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
        chooser.setDialogTitle("Save shapefile");
        chooser.setSelectedFile(new File(newPath));

        int returnVal = chooser.showSaveDialog(null);

        if (returnVal != JFileDataStoreChooser.APPROVE_OPTION) {
           
           return null;
        }

        File newFile = chooser.getSelectedFile();
        if (newFile.equals(csvFile)) {
            System.out.println("Error: cannot replace " + csvFile);
            System.exit(0);
        }

        return newFile;    //returns the new shapefile
    }
     //method converts csv files to shapefiles...
    public  void Csv2Shp() throws Exception {
        //sets the look and feel of the gui
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

            //prompts the user to choose the csv file to convert to shapefile
        File file = JFileDataStoreChooser.showOpenFile("csv", null);
        if (file == null) {
            return;
        }
     final SimpleFeatureType TYPE = DataUtilities.createType("Location",
                "the_geom:Point:srid=4326," + // <- the geometry attribute: Point type
                "name:String," +   // <- a String attribute
                "number:Integer"   // a number attribute
        );
         System.out.println("TYPE:"+TYPE);
         List<SimpleFeature> features = new ArrayList<>();
        
        /*
         * GeometryFactory will be used to create the geometry attribute of each feature,
         * using a Point object for the location.
         */
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

        try (BufferedReader reader = new BufferedReader(new FileReader(file)) ){
            /* First line of the data file is the header */
            String line = reader.readLine();
            System.out.println("Header: " + line);

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.trim().length() > 0) { // skip blank lines
                    String tokens[] = line.split("\\,");

                    double latitude = Double.parseDouble(tokens[0]);
                    double longitude = Double.parseDouble(tokens[1]);
                    String name = tokens[2].trim();
                    int number = Integer.parseInt(tokens[3].trim());

                    /* Longitude (= x coord) first ! */
                    Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

                    featureBuilder.add(point);
                    featureBuilder.add(name);
                    featureBuilder.add(number);
                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    features.add(feature);
                }
            }
        }
         File newFile = getNewShapeFile(file);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

        /*
         * TYPE is used as a template to describe the file contents
         */
        newDataStore.createSchema(TYPE);
            Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource1 = newDataStore.getFeatureSource(typeName);
        SimpleFeatureType SHAPE_TYPE = featureSource1.getSchema();
        /*
         * The Shapefile format has a couple limitations:
         * - "the_geom" is always first, and used for the geometry attribute name
         * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
         * - Attribute names are limited in length 
         * - Not all data types are supported (example Timestamp represented as Date)
         * 
         * Each data store has different limitations so check the resulting SimpleFeatureType.
         */
        System.out.println("SHAPE:"+SHAPE_TYPE);

        if (featureSource1 instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource1;
            /*
             * SimpleFeatureStore has a method to add features from a
             * SimpleFeatureCollection object, so we use the ListFeatureCollection
             * class to wrap our list of features.
             */
      SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();
            } catch (IOException problem) {
                transaction.rollback();
            } finally {
                transaction.close();
            }
           // System.exit(0); // success!
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }
        
      
    }
    
    //this method creates a map context for displaying vector data
    private void mapContext() throws Exception {
        
        map = new DefaultMapContext();//map to display the vector data
      
        // Create a JMapFrame with custom toolbar buttons
        JMapFrame mapFrame = new JMapFrame(map);  //mapFrame to add the map to
        mapFrame.enableToolBar(true);    //toolbar enabled 
         mapFrame.enableStatusBar(true);//status bar enabled
       mapFrame.enableLayerTable(true);  //layer table enabled
      

        JToolBar toolbar = mapFrame.getToolBar();
       
        toolbar.add(new JButton(new OpenShapefileAction()));  //toolbar button open a shapefile
        toolbar.add(new JButton(new ExportShapefileAction1()));// toolbar button to export a shapefile
      
        mapFrame.setSize(800, 600);
        mapFrame.setVisible(true);
    }
    
    
    //inner class: with an action to export a shapefile with transformed coordinates
class ExportShapefileAction1 extends SafeAction {
       ExportShapefileAction1() {
            super("Export shapefile...");  //name displayed on created button
            putValue(Action.SHORT_DESCRIPTION, "Export shapefile with the current coordinate system");  //words displayed when cursor moves over the button
        }
        @Override
        public void action(ActionEvent e) throws Throwable {
            SimpleFeatureType schema = featureSource.getSchema();
             JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
        chooser.setDialogTitle("Save reprojected shapefile");
         chooser.setSaveFile(sourceFile);
          int returnVal = chooser.showSaveDialog(null);
        if (returnVal != JFileDataStoreChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file.equals(sourceFile)) {
            JOptionPane.showMessageDialog(null, "Cannot replace " + file);
            return;
        }
           CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
        CoordinateReferenceSystem worldCRS = map.getCoordinateReferenceSystem();
        boolean lenient = true; // allow for some error due to different datums
        MathTransform transform = CRS.findMathTransform(dataCRS, worldCRS, lenient);
         SimpleFeatureCollection featureCollection = featureSource.getFeatures();

         DataStoreFactorySpi factory = new ShapefileDataStoreFactory();
        Map<String, Serializable> create = new HashMap<>();
        create.put("url", file.toURI().toURL());
        create.put("create spatial index", Boolean.TRUE);
        DataStore dataStore = factory.createNewDataStore(create);
        SimpleFeatureType featureType = SimpleFeatureTypeBuilder.retype(schema, worldCRS);
        dataStore.createSchema(featureType);
  String createdName = dataStore.getTypeNames()[0];

Transaction transaction = new DefaultTransaction("Reproject");
        try ( FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                        dataStore.getFeatureWriterAppend(createdName, transaction);
              SimpleFeatureIterator iterator = featureCollection.features()){
            while (iterator.hasNext()) {
                // copy the contents of each feature and transform the geometry
                SimpleFeature feature = iterator.next();
                SimpleFeature copy = writer.next();
                copy.setAttributes(feature.getAttributes());

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Geometry geometry2 = JTS.transform(geometry, transform);

                copy.setDefaultGeometry(geometry2);
                writer.write();
            }
            transaction.commit();
            JOptionPane.showMessageDialog(null, "Export to shapefile complete");
        } catch (Exception problem) {
            problem.printStackTrace();
            transaction.rollback();
            JOptionPane.showMessageDialog(null, "Export to shapefile failed");
        } finally {
            transaction.close();
        }
           
        }
    }

//inner class with an action to open a shapefile and add it to the mapcontext
class OpenShapefileAction extends SafeAction {
        OpenShapefileAction() {
            super("Open shapefile...");
            putValue(Action.SHORT_DESCRIPTION, "Import shapefile");
        }
        @Override
        public void action(ActionEvent e) throws Throwable {
               sourceFile = JFileDataStoreChooser.showOpenFile("shp", null);
        if (sourceFile == null) {
            return;
        }
        FileDataStore store = FileDataStoreFinder.getDataStore(sourceFile);
        featureSource = store.getFeatureSource();

        // Create a map context and add our shapefile to it
       // map = new DefaultMapContext();
        map.addLayer(featureSource, null);
     
        }
    }
  
//constructor with the default application when the program is run
    public Trial() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  //when the user closes the application
        getContentPane().setLayout(new BorderLayout());

        text = new JTextField(80);
        text.setText("include"); // include selects everything!
        getContentPane().add(text, BorderLayout.NORTH);
        
    //create a table and add it to the jframe
        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(new DefaultTableModel(5, 5));
        table.setPreferredScrollableViewportSize(new Dimension(500, 200));
//create a scroll bar
        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
    
    //creating a menubar to add menu items
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);
//creating a menu item with "File" as a name
        JMenu fileMenu = new JMenu("File");
        menubar.add(fileMenu);//add "File" to the menu bar

        featureTypeCBox = new JComboBox<>();
        menubar.add(featureTypeCBox);
//creating a menu item with "Data" as a name
        JMenu dataMenu = new JMenu("Data");
          menubar.add(dataMenu);//add "Data" to the menubar
        pack();
        
        //adding items under the "File" menu with their actions
        fileMenu.add(new SafeAction("Open shapefile...") { 
            @Override
            public void action(ActionEvent e) throws Throwable {
                connect(new ShapefileDataStoreFactory()); // to query the data from the shapefile
            }
        });
        
        fileMenu.add(new SafeAction("Open MapContext...") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                mapContext(); // creates the map context to add shapefiles and do coordinate transformation
            }
        });
         fileMenu.add(new SafeAction("Create shapefile...") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                Csv2Shp();   //method converts csv files to shapefiles
            }
        });
        
             
        fileMenu.addSeparator();
        fileMenu.add(new SafeAction("Exit") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                System.exit(0);   //to exit if the user presses the button
            }
        });
        dataMenu.add(new SafeAction("Get features") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                filterFeatures();   //to query the features from the added shapefile
            }
        });
        dataMenu.add(new SafeAction("Count") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                countFeatures();  //to count the number of features added in the table
            }
        });
       
}
    
    //method to query the data 
    public void connect(DataStoreFactorySpi format) throws Exception {
        JDataStoreWizard wizard = new JDataStoreWizard(format);
        int result = wizard.showModalDialog();
        if (result == JWizard.FINISH) {
            Map<String, Object> connectionParameters = wizard.getConnectionParameters();
            dataStore = DataStoreFinder.getDataStore(connectionParameters);
            if (dataStore == null) {
                JOptionPane.showMessageDialog(null, "Could not connect - check parameters");
            }
            updateUI();
        }
    }
     private void updateUI() throws Exception {
        ComboBoxModel<String> cbm = new DefaultComboBoxModel<>(dataStore.getTypeNames());
        featureTypeCBox.setModel(cbm);

        table.setModel(new DefaultTableModel(5, 5));
    }
     
     //method that adds the data to the table
      private void filterFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = CQL.toFilter(text.getText());
        SimpleFeatureCollection features = source.getFeatures(filter);
        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
      
      //method to count the number of features added
       private void countFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = CQL.toFilter(text.getText());
        SimpleFeatureCollection features = source.getFeatures(filter);

        int count = features.size();
        JOptionPane.showMessageDialog(text, "Number of selected features:" + count);
    }
       //method to query the data from the shapefile
        private void queryFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        FeatureType schema = source.getSchema();
        String name = schema.getGeometryDescriptor().getLocalName();

        Filter filter = CQL.toFilter(text.getText());

        Query query = new Query(typeName, filter, new String[] { name });

        SimpleFeatureCollection features = source.getFeatures(query);

        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
        
        //main method
    public static void main(String[] args) throws Exception {
        JFrame frame = new Trial();
        frame.setVisible(true);
        frame.setTitle("YEAR 1 GIS");
    }
}
    