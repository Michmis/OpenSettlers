package soc.gwtClient.editor;

import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.SimpleRadioButton;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class EditTerritory extends DialogBox
{

    public EditTerritory()
    {
        setWidth("617px");
        setHTML("Territory properties");
        
        DockPanel dockPanel = new DockPanel();
        setWidget(dockPanel);
        dockPanel.setSize("598px", "397px");
        
        HorizontalPanel horizontalPanel = new HorizontalPanel();
        dockPanel.add(horizontalPanel, DockPanel.EAST);
        
        VerticalPanel variants = new VerticalPanel();
        horizontalPanel.add(variants);
        
        CheckBox chckbxStandard = new CheckBox("Standard");
        variants.add(chckbxStandard);
        
        CheckBox chckbxSeafarers = new CheckBox("SeaFarers");
        variants.add(chckbxSeafarers);
        
        CheckBox chckbxCitiesKnights = new CheckBox("Cities & Knights");
        variants.add(chckbxCitiesKnights);
        
        CheckBox chckbxPioneers = new CheckBox("Pioneers");
        variants.add(chckbxPioneers);
        
        CheckBox chckbxSead = new CheckBox("Sea3D");
        variants.add(chckbxSead);
        
        CheckBox chckbxExtended = new CheckBox("Extended");
        variants.add(chckbxExtended);
        
        VerticalPanel hexList = new VerticalPanel();
        horizontalPanel.add(hexList);
        
        CellList cellList = new CellList(new TextCell());
        horizontalPanel.add(cellList);
        cellList.setWidth("97px");
        
        VerticalPanel portList = new VerticalPanel();
        horizontalPanel.add(portList);
        
        VerticalPanel chitList = new VerticalPanel();
        horizontalPanel.add(chitList);
        
        VerticalPanel verticalPanel = new VerticalPanel();
        dockPanel.add(verticalPanel, DockPanel.WEST);
        
        CellList territoryList = new CellList(new TextCell());
        verticalPanel.add(territoryList);
        territoryList.setWidth("85px");
    }

}
