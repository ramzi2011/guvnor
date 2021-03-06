package org.drools.guvnor.client.explorer.navigation.reporting;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.IsWidget;
import org.drools.guvnor.client.explorer.navigation.NavigationItemBuilder;
import org.drools.guvnor.client.explorer.navigation.NavigationViewFactory;

public class ReportingNavigationItemBuilder extends NavigationItemBuilder {

    private NavigationViewFactory navigationViewFactory;
    private PlaceController placeController;

    public ReportingNavigationItemBuilder(NavigationViewFactory navigationViewFactory, PlaceController placeController) {
        this.navigationViewFactory = navigationViewFactory;
        this.placeController = placeController;
    }

    @Override
    public boolean hasPermissionToBuild() {
        return true;
    }

    @Override
    public IsWidget getHeader() {
        return navigationViewFactory.getReportingHeaderView();
    }

    @Override
    public IsWidget getContent() {
        return new ReportingTree(navigationViewFactory.getReportingTreeView(), placeController);
    }
}
