package net.jeeeyul.pdetools.clipboard.internal;

import java.util.ArrayList;
import java.util.List;

import net.jeeeyul.pdetools.clipboard.ClipboardViewer;
import net.jeeeyul.pdetools.clipboard.IClipboardService;
import net.jeeeyul.pdetools.model.pdetools.provider.PdetoolsItemProviderAdapterFactory;
import net.jeeeyul.pdetools.shared.UpdateJob;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;

public class ClipboardView extends ViewPart {
	public static final String ID = "net.jeeeyul.pdetools.clipboard.ClipboardView";
	private ClipboardViewer viewer;
	private List<ClipboardHistoryAction> actions = new ArrayList<ClipboardHistoryAction>();
	private EContentAdapter listener = new EContentAdapter() {
		public void notifyChanged(Notification notification) {
			if (notification.isTouch()) {
				return;
			}
			updateJob.schedule();
		};
	};

	private UpdateJob updateJob = new UpdateJob(new Procedure0() {
		@Override
		public void apply() {
			updateActions();
		}
	});

	private void configureActions() {
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(viewer.getTableViewer().getTable());
		viewer.getTableViewer().getTable().setMenu(menu);
		getViewSite().registerContextMenu(menuManager, viewer.getTableViewer());

		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		RemoveAllAction removeAllAction = new RemoveAllAction(getClipboardService().getHistory());
		actions.add(removeAllAction);
		toolBarManager.add(removeAllAction);

		UndoAction undoAction = new UndoAction(getClipboardService().getHistory());
		actions.add(undoAction);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);

		RedoAction redoAction = new RedoAction(getClipboardService().getHistory());
		actions.add(redoAction);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NORMAL);
		GridLayout layout = new GridLayout();

		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 2;
		container.setLayout(layout);

		viewer = new ClipboardViewer(container, SWT.MULTI);
		viewer.getTableViewer().getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		TableViewer tableViewer = viewer.getTableViewer();
		getViewSite().setSelectionProvider(tableViewer);

		configureActions();

		getClipboardService().getHistory().eAdapters().add(listener);
		updateActions();
	}

	@Override
	public void dispose() {
		getClipboardService().getHistory().eAdapters().remove(listener);
		super.dispose();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == IPropertySheetPage.class) {
			PropertySheetPage page = new PropertySheetPage();
			page.setPropertySourceProvider(new AdapterFactoryContentProvider(new PdetoolsItemProviderAdapterFactory()));
			return page;
		}

		return super.getAdapter(adapter);
	}

	private IClipboardService getClipboardService() {
		return (IClipboardService) getSite().getService(IClipboardService.class);
	}

	public ClipboardViewer getViewer() {
		return viewer;
	}

	@Override
	public void setFocus() {
		viewer.getTableViewer().getTable().setFocus();
	}

	protected void updateActions() {
		for (ClipboardHistoryAction eachAction : actions) {
			eachAction.update();
		}
	}
}
